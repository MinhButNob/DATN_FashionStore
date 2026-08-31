package com.example.backend_fashionstore.QuanLyDonHang.repository;

import com.example.backend_fashionstore.entity.Order;
import com.example.backend_fashionstore.entity.Customer;
import com.example.backend_fashionstore.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Lấy đơn hàng theo customer
    Page<Order> findByCustomerAndDeletedAtIsNull(Customer customer, Pageable pageable);

    // Dùng OrderStatus enum
    Page<Order> findByCustomerAndOrderStatusAndDeletedAtIsNull(Customer customer, OrderStatus status, Pageable pageable);

    // Lấy tất cả đơn hàng (cho admin)
    @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL ORDER BY o.createdAt DESC")
    Page<Order> findAllActiveOrders(Pageable pageable);

    // Filter theo status - dùng enum
    @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL " +
            "AND (:status IS NULL OR o.orderStatus = :status) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> filterOrders(
            @Param("status") OrderStatus status,
            Pageable pageable
    );

    // Tìm kiếm theo keyword
    @Query(value = "SELECT * FROM orders o WHERE o.order_deleted_at IS NULL " +
            "AND (:keyword IS NULL OR " +
            "CAST(o.order_id AS VARCHAR) LIKE CONCAT('%', :keyword, '%') OR " +
            "o.order_customer_id IN (SELECT c.customer_id FROM customers c WHERE c.customer_name ILIKE CONCAT('%', :keyword, '%'))) " +
            "ORDER BY o.order_created_at DESC",
            nativeQuery = true)
    Page<Order> searchOrdersNative(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // Filter với cả status và keyword - dùng enum
    @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL " +
            "AND (:status IS NULL OR o.orderStatus = :status) " +
            "AND (:keyword IS NULL OR " +
            "CAST(o.orderId AS string) LIKE CONCAT('%', :keyword, '%') OR " +
            "o.customer.customerName LIKE CONCAT('%', :keyword, '%')) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> filterOrdersWithKeyword(
            @Param("status") OrderStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // Lấy đơn hàng theo customer id
    @Query("SELECT o FROM Order o WHERE o.customer.customerId = :customerId AND o.deletedAt IS NULL ORDER BY o.createdAt DESC")
    Page<Order> findByCustomerId(@Param("customerId") Integer customerId, Pageable pageable);

    // 🆕 Lấy đơn hàng theo danh sách orderId (dùng cho filter REJECTED)
    @Query("SELECT o FROM Order o WHERE o.orderId IN :orderIds AND o.deletedAt IS NULL ORDER BY o.createdAt DESC")
    Page<Order> findByOrderIdIn(@Param("orderIds") List<Long> orderIds, Pageable pageable);

    // Thống kê đơn hàng theo trạng thái - dùng enum
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deletedAt IS NULL AND o.orderStatus = :status")
    long countByOrderStatus(@Param("status") OrderStatus status);

    // Doanh thu hôm nay
    @Query(value = "SELECT COALESCE(SUM(order_total_amount - COALESCE(order_refund_amount, 0)), 0) FROM orders WHERE order_deleted_at IS NULL AND order_status IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED', 'REJECTED') AND DATE(order_created_at) = CURRENT_DATE", nativeQuery = true)
    BigDecimal getTodayRevenue();

    // ===== THỐNG KÊ DOANH THU MỚI =====

    /**
     * Tổng doanh thu từ tất cả đơn hàng COMPLETED và các đơn có yêu cầu hoàn (chưa xác nhận)
     */
    @Query(value = "SELECT COALESCE(SUM(order_total_amount - COALESCE(order_refund_amount, 0)), 0) FROM orders " +
            "WHERE order_deleted_at IS NULL AND order_status IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED', 'REJECTED')",
            nativeQuery = true)
    BigDecimal getTotalRevenue();

    /**
     * Doanh thu online từ đơn hàng COMPLETED và các đơn có yêu cầu hoàn (chưa xác nhận)
     * Bảng orders chỉ chứa đơn online — pos_orders là bảng riêng, không cần lọc NOT IN
     */
    @Query(value = "SELECT COALESCE(SUM(order_total_amount - COALESCE(order_refund_amount, 0)), 0) FROM orders o " +
            "WHERE o.order_deleted_at IS NULL " +
            "AND o.order_status IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED', 'REJECTED')",
            nativeQuery = true)
    BigDecimal getOnlineRevenue();

    /**
     * Tổng tiền hoàn trả từ đơn hàng RETURNED và PARTIALLY_RETURNED
     */
    @Query(value = "SELECT COALESCE(SUM(COALESCE(order_refund_amount, 0)), 0) FROM orders " +
            "WHERE order_deleted_at IS NULL AND order_status IN ('RETURNED', 'PARTIALLY_RETURNED')",
            nativeQuery = true)
    BigDecimal getReturnRefundAmount();

    /**
     * Doanh thu theo tháng
     */
    @Query(value = "SELECT COALESCE(SUM(order_total_amount - COALESCE(order_refund_amount, 0)), 0) FROM orders " +
            "WHERE order_deleted_at IS NULL AND order_status IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED', 'REJECTED') " +
            "AND EXTRACT(YEAR FROM order_created_at) = :year " +
            "AND EXTRACT(MONTH FROM order_created_at) = :month",
            nativeQuery = true)
    BigDecimal getMonthlyRevenue(@Param("year") int year, @Param("month") int month);

    /**
     * Đếm đơn hàng hủy theo loại
     */
    @Query(value = "SELECT COUNT(*) FROM orders " +
            "WHERE order_deleted_at IS NULL AND order_status = 'CANCELLED' " +
            "AND order_notes LIKE CONCAT('%', :type, '%')",
            nativeQuery = true)
    long countCancelledByType(@Param("type") String type);

    // Kiểm tra đơn hàng đã COMPLETED
    @Query(value = "SELECT COUNT(*) > 0 FROM orders o " +
            "JOIN order_details od ON o.order_id = od.order_detail_order_id " +
            "WHERE o.order_customer_id = :customerId " +
            "AND od.order_detail_product_detail_id = :productDetailId " +
            "AND o.order_status = 'COMPLETED' " +
            "AND o.order_deleted_at IS NULL",
            nativeQuery = true)
    boolean hasCustomerCompletedOrder(
            @Param("customerId") Integer customerId,
            @Param("productDetailId") Integer productDetailId
    );

    // ===== DASHBOARD - THÊM MỚI =====

    /**
     * Lấy doanh thu theo từng tháng trong năm
     */
    @Query(value = """
        SELECT 
            EXTRACT(MONTH FROM o.order_created_at) as month,
            COALESCE(SUM(o.order_total_amount - COALESCE(o.order_refund_amount, 0)), 0) as revenue
        FROM orders o
        WHERE o.order_deleted_at IS NULL 
            AND o.order_status IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED', 'REJECTED')
            AND EXTRACT(YEAR FROM o.order_created_at) = :year
        GROUP BY EXTRACT(MONTH FROM o.order_created_at)
        ORDER BY month ASC
        """, nativeQuery = true)
    List<Object[]> getMonthlyRevenueByYear(@Param("year") int year);

    /**
     * Lấy doanh thu tháng hiện tại
     */
    @Query(value = """
        SELECT COALESCE(SUM(order_total_amount - COALESCE(order_refund_amount, 0)), 0) 
        FROM orders 
        WHERE order_deleted_at IS NULL 
            AND order_status IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED', 'REJECTED')
            AND EXTRACT(YEAR FROM order_created_at) = EXTRACT(YEAR FROM CURRENT_DATE)
            AND EXTRACT(MONTH FROM order_created_at) = EXTRACT(MONTH FROM CURRENT_DATE)
        """, nativeQuery = true)
    BigDecimal getCurrentMonthRevenue();

    /**
     * Lấy doanh thu tháng trước
     */
    @Query(value = """
        SELECT COALESCE(SUM(order_total_amount - COALESCE(order_refund_amount, 0)), 0) 
        FROM orders 
        WHERE order_deleted_at IS NULL 
            AND order_status IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED', 'REJECTED')
            AND EXTRACT(YEAR FROM order_created_at) = EXTRACT(YEAR FROM CURRENT_DATE - INTERVAL '1 month')
            AND EXTRACT(MONTH FROM order_created_at) = EXTRACT(MONTH FROM CURRENT_DATE - INTERVAL '1 month')
        """, nativeQuery = true)
    BigDecimal getPreviousMonthRevenue();

    /**
     * Lấy 5 đơn hàng gần đây nhất
     */
    @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL ORDER BY o.createdAt DESC")
    List<Order> findTop5ByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Lấy tổng số đơn hàng COMPLETED
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deletedAt IS NULL AND o.orderStatus = 'COMPLETED'")
    long countCompletedOrders();

    /**
     * Lấy tổng số đơn hàng CANCELLED
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deletedAt IS NULL AND o.orderStatus = 'CANCELLED'")
    long countCancelledOrders();

    /**
     * Lấy tổng số đơn hàng PENDING_PAYMENT
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deletedAt IS NULL AND o.orderStatus = 'PENDING_PAYMENT'")
    long countPendingOrders();

    /**
     * Lấy tổng số đơn hàng PROCESSING
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deletedAt IS NULL AND o.orderStatus = 'PROCESSING'")
    long countProcessingOrders();

    /**
     * Lấy tổng số đơn hàng SHIPPING
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deletedAt IS NULL AND o.orderStatus = 'SHIPPING'")
    long countShippingOrders();

    /**
     * Lấy tổng số đơn hàng DELIVERED
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deletedAt IS NULL AND o.orderStatus = 'DELIVERED'")
    long countDeliveredOrders();

    /**
     * Lấy tổng số đơn hàng CONFIRMED
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deletedAt IS NULL AND o.orderStatus = 'CONFIRMED'")
    long countConfirmedOrders();

    /**
     * Lấy doanh thu theo ngày trong tháng
     */
    @Query(value = """
        SELECT 
            EXTRACT(DAY FROM o.order_created_at) as day,
            COALESCE(SUM(o.order_total_amount - COALESCE(o.order_refund_amount, 0)), 0) as revenue
        FROM orders o
        WHERE o.order_deleted_at IS NULL 
            AND o.order_status IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED', 'REJECTED')
            AND EXTRACT(YEAR FROM o.order_created_at) = :year
            AND EXTRACT(MONTH FROM o.order_created_at) = :month
        GROUP BY EXTRACT(DAY FROM o.order_created_at)
        ORDER BY day ASC
        """, nativeQuery = true)
    List<Object[]> getDailyRevenueByMonth(
            @Param("year") int year,
            @Param("month") int month
    );

    /**
     * Thống kê trạng thái đơn hàng theo khoảng thời gian
     */
    @Query(value = """
        SELECT 
            o.order_status as status,
            COUNT(o.order_id) as count
        FROM orders o
        WHERE o.order_deleted_at IS NULL 
            AND o.order_created_at >= :startDate 
            AND o.order_created_at <= :endDate
        GROUP BY o.order_status
        """, nativeQuery = true)
    List<Object[]> countOrderStatusByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}