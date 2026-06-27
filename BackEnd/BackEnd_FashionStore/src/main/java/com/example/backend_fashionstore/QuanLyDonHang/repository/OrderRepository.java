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
    @Query(value = "SELECT COALESCE(SUM(order_total_amount), 0) FROM orders WHERE order_deleted_at IS NULL AND order_status = 'COMPLETED' AND DATE(order_created_at) = CURRENT_DATE", nativeQuery = true)
    BigDecimal getTodayRevenue();

    // Doanh thu theo tháng
    @Query(value = "SELECT COALESCE(SUM(order_total_amount), 0) FROM orders WHERE order_deleted_at IS NULL AND order_status = 'COMPLETED' AND EXTRACT(YEAR FROM order_created_at) = :year AND EXTRACT(MONTH FROM order_created_at) = :month", nativeQuery = true)
    BigDecimal getMonthlyRevenue(@Param("year") int year, @Param("month") int month);

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
}