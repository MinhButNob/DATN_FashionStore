// repository/StatisticsRepository.java
package com.example.backend_fashionstore.QuanLyThongKe.repository;

import com.example.backend_fashionstore.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StatisticsRepository extends JpaRepository<Order, Long> {

    // =========================================
    // 1. THỐNG KÊ ĐƠN HÀNG ONLINE (orders) - CHỈ COMPLETED
    // =========================================

    /**
     * Doanh thu online theo khoảng thời gian (chỉ COMPLETED)
     */
    @Query("SELECT COALESCE(SUM(o.orderTotalAmount - CASE WHEN o.orderStatus = 'RETURNED' THEN COALESCE(o.refundAmount, 0) ELSE 0 END), 0) FROM Order o " +
            "WHERE o.orderStatus IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.deletedAt IS NULL " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getOnlineRevenue(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    /**
     * Số đơn hàng online theo khoảng thời gian (chỉ COMPLETED)
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.orderStatus IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.deletedAt IS NULL " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    Long getOnlineOrderCount(@Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    /**
     * Số khách hàng online theo khoảng thời gian (chỉ COMPLETED)
     */
    @Query("SELECT COUNT(DISTINCT o.customer.customerId) FROM Order o " +
            "WHERE o.orderStatus IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.deletedAt IS NULL " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    Long getOnlineCustomerCount(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    // =========================================
    // 2. THỐNG KÊ ĐƠN HÀNG POS (pos_orders) - TẤT CẢ ĐƠN HỢP LỆ
    // =========================================

    /**
     * Doanh thu POS theo khoảng thời gian
     */
    @Query(value = "SELECT COALESCE(SUM(COALESCE(NULLIF(po.pos_order_total_amount, 0), NULLIF(po.pos_order_paid_amount, 0), NULLIF(po.pos_order_subtotal - COALESCE(po.pos_order_discount_amount, 0), 0), (SELECT COALESCE(SUM(poi.pos_order_item_total), 0) FROM pos_order_items poi WHERE poi.pos_order_item_order_id = po.pos_order_id), 0)), 0) FROM pos_orders po " +
            "WHERE (po.pos_order_status IS NULL OR UPPER(po.pos_order_status) NOT IN ('CANCELLED', 'DELETED')) " +
            "AND po.pos_order_created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    BigDecimal getPosRevenue(@Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    /**
     * Số đơn hàng POS theo khoảng thời gian
     */
    @Query(value = "SELECT COUNT(po.pos_order_id) FROM pos_orders po " +
            "WHERE (po.pos_order_status IS NULL OR UPPER(po.pos_order_status) NOT IN ('CANCELLED', 'DELETED')) " +
            "AND po.pos_order_created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    Long getPosOrderCount(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    /**
     * Số khách hàng POS theo khoảng thời gian
     */
    @Query(value = "SELECT COUNT(DISTINCT po.pos_order_customer_id) FROM pos_orders po " +
            "WHERE (po.pos_order_status IS NULL OR UPPER(po.pos_order_status) NOT IN ('CANCELLED', 'DELETED')) " +
            "AND po.pos_order_customer_id IS NOT NULL " +
            "AND po.pos_order_created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    Long getPosCustomerCount(@Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    // =========================================
    // 3. PHƯƠNG THỨC CHO TĂNG TRƯỞNG
    // =========================================

    /**
     * Doanh thu online cho tăng trưởng (chỉ COMPLETED)
     */
    @Query("SELECT COALESCE(SUM(o.orderTotalAmount - CASE WHEN o.orderStatus = 'RETURNED' THEN COALESCE(o.refundAmount, 0) ELSE 0 END), 0) FROM Order o " +
            "WHERE o.orderStatus IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.deletedAt IS NULL " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getOnlineRevenueBetween(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Số đơn hàng online cho tăng trưởng (chỉ COMPLETED)
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.orderStatus IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.deletedAt IS NULL " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    Long getOnlineOrderCountBetween(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Doanh thu POS cho tăng trưởng
     */
    @Query(value = "SELECT COALESCE(SUM(COALESCE(NULLIF(po.pos_order_total_amount, 0), NULLIF(po.pos_order_paid_amount, 0), NULLIF(po.pos_order_subtotal - COALESCE(po.pos_order_discount_amount, 0), 0), (SELECT COALESCE(SUM(poi.pos_order_item_total), 0) FROM pos_order_items poi WHERE poi.pos_order_item_order_id = po.pos_order_id), 0)), 0) FROM pos_orders po " +
            "WHERE (po.pos_order_status IS NULL OR UPPER(po.pos_order_status) NOT IN ('CANCELLED', 'DELETED')) " +
            "AND po.pos_order_created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    BigDecimal getPosRevenueBetween(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Số đơn hàng POS cho tăng trưởng
     */
    @Query(value = "SELECT COUNT(po.pos_order_id) FROM pos_orders po " +
            "WHERE (po.pos_order_status IS NULL OR UPPER(po.pos_order_status) NOT IN ('CANCELLED', 'DELETED')) " +
            "AND po.pos_order_created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    Long getPosOrderCountBetween(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    // =========================================
    // 4. DOANH THU HÔM NAY
    // =========================================

    @Query(value = "SELECT COALESCE(SUM(o.order_total_amount - CASE WHEN o.order_status = 'RETURNED' THEN COALESCE(o.order_refund_amount, 0) ELSE 0 END), 0) FROM orders o " +
            "WHERE o.order_status IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.order_deleted_at IS NULL " +
            "AND DATE(o.order_created_at) = CURRENT_DATE", nativeQuery = true)
    BigDecimal getTodayOnlineRevenue();

    @Query(value = "SELECT COUNT(o.order_id) FROM orders o " +
            "WHERE o.order_status IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.order_deleted_at IS NULL " +
            "AND DATE(o.order_created_at) = CURRENT_DATE", nativeQuery = true)
    Long getTodayOnlineOrders();

    @Query(value = "SELECT COALESCE(SUM(COALESCE(NULLIF(po.pos_order_total_amount, 0), NULLIF(po.pos_order_paid_amount, 0), NULLIF(po.pos_order_subtotal - COALESCE(po.pos_order_discount_amount, 0), 0), (SELECT COALESCE(SUM(poi.pos_order_item_total), 0) FROM pos_order_items poi WHERE poi.pos_order_item_order_id = po.pos_order_id), 0)), 0) FROM pos_orders po " +
            "WHERE (po.pos_order_status IS NULL OR UPPER(po.pos_order_status) NOT IN ('CANCELLED', 'DELETED')) " +
            "AND DATE(po.pos_order_created_at) = CURRENT_DATE", nativeQuery = true)
    BigDecimal getTodayPosRevenue();

    @Query(value = "SELECT COUNT(po.pos_order_id) FROM pos_orders po " +
            "WHERE (po.pos_order_status IS NULL OR UPPER(po.pos_order_status) NOT IN ('CANCELLED', 'DELETED')) " +
            "AND DATE(po.pos_order_created_at) = CURRENT_DATE", nativeQuery = true)
    Long getTodayPosOrders();

    // =========================================
    // 5. DOANH THU THEO NGÀY
    // =========================================

    @Query(value = "SELECT DATE(o.order_created_at) as date, " +
            "COALESCE(SUM(o.order_total_amount - CASE WHEN o.order_status = 'RETURNED' THEN COALESCE(o.order_refund_amount, 0) ELSE 0 END), 0) as revenue, " +
            "COUNT(o.order_id) as orders " +
            "FROM orders o " +
            "WHERE o.order_status IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.order_deleted_at IS NULL " +
            "AND o.order_created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(o.order_created_at) " +
            "ORDER BY DATE(o.order_created_at) ASC", nativeQuery = true)
    List<Object[]> getDailyOnlineRevenue(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT DATE(po.pos_order_created_at) as date, " +
            "COALESCE(SUM(COALESCE(NULLIF(po.pos_order_total_amount, 0), NULLIF(po.pos_order_paid_amount, 0), NULLIF(po.pos_order_subtotal - COALESCE(po.pos_order_discount_amount, 0), 0), (SELECT COALESCE(SUM(poi.pos_order_item_total), 0) FROM pos_order_items poi WHERE poi.pos_order_item_order_id = po.pos_order_id), 0)), 0) as revenue, " +
            "COUNT(po.pos_order_id) as orders " +
            "FROM pos_orders po " +
            "WHERE (po.pos_order_status IS NULL OR UPPER(po.pos_order_status) NOT IN ('CANCELLED', 'DELETED')) " +
            "AND po.pos_order_created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(po.pos_order_created_at) " +
            "ORDER BY DATE(po.pos_order_created_at) ASC", nativeQuery = true)
    List<Object[]> getDailyPosRevenue(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    // =========================================
    // 6. SẢN PHẨM BẢN CHẠY
    // =========================================

    @Query(value = "SELECT " +
            "p.product_id, " +
            "p.product_name, " +
            "c.category_name, " +
            "SUM(od.order_detail_quantity) as total_quantity, " +
            "SUM(od.order_detail_final_amount) as total_revenue " +
            "FROM order_details od " +
            "JOIN orders o ON od.order_detail_order_id = o.order_id " +
            "JOIN product_details pd ON od.order_detail_product_detail_id = pd.product_detail_id " +
            "JOIN products p ON pd.product_detail_product_id = p.product_id " +
            "LEFT JOIN categories c ON p.product_category_id = c.category_id " +
            "WHERE o.order_status IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.order_deleted_at IS NULL " +
            "AND o.order_created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY p.product_id, p.product_name, c.category_name " +
            "ORDER BY total_revenue DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> getTopSellingProducts(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         @Param("limit") int limit);

    @Query(value = "SELECT " +
            "p.product_id, " +
            "p.product_name, " +
            "c.category_name, " +
            "SUM(pi.pos_order_item_quantity) as total_quantity, " +
            "SUM(pi.pos_order_item_total) as total_revenue " +
            "FROM pos_order_items pi " +
            "JOIN pos_orders po ON pi.pos_order_item_order_id = po.pos_order_id " +
            "JOIN product_details pd ON pi.pos_order_item_product_detail_id = pd.product_detail_id " +
            "JOIN products p ON pd.product_detail_product_id = p.product_id " +
            "LEFT JOIN categories c ON p.product_category_id = c.category_id " +
            "WHERE (po.pos_order_status IS NULL OR UPPER(po.pos_order_status) NOT IN ('CANCELLED', 'DELETED')) " +
            "AND po.pos_order_created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY p.product_id, p.product_name, c.category_name " +
            "ORDER BY total_revenue DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> getTopSellingPosProducts(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            @Param("limit") int limit);

    // =========================================
    // 7. NHÂN VIÊN XUẤT SẮC (POS & ONLINE)
    // =========================================

    @Query(value = "SELECT " +
            "COALESCE(e.employee_id, e2.employee_id, ps.pos_session_employee_id, 1) as employee_id, " +
            "COALESCE(NULLIF(NULLIF(e.employee_fullname, ''), 'Ko có tên'), NULLIF(NULLIF(e2.employee_fullname, ''), 'Ko có tên'), NULLIF(u.username, ''), CONCAT('Nhân viên #', COALESCE(e.employee_id, e2.employee_id, ps.pos_session_employee_id, 1))) as employee_name, " +
            "COUNT(DISTINCT po.pos_order_id) as total_orders, " +
            "COALESCE(SUM(COALESCE(NULLIF(po.pos_order_total_amount, 0), NULLIF(po.pos_order_paid_amount, 0), NULLIF(po.pos_order_subtotal - COALESCE(po.pos_order_discount_amount, 0), 0), 0)), 0) as total_revenue " +
            "FROM pos_orders po " +
            "LEFT JOIN pos_sessions ps ON po.pos_order_session_id = ps.pos_session_id " +
            "LEFT JOIN employees e ON ps.pos_session_employee_id = e.employee_id " +
            "LEFT JOIN employees e2 ON ps.pos_session_employee_id = e2.employee_user_id " +
            "LEFT JOIN users u ON COALESCE(e.employee_user_id, e2.employee_user_id) = u.user_id " +
            "WHERE (po.pos_order_status IS NULL OR UPPER(po.pos_order_status) NOT IN ('CANCELLED', 'DELETED')) " +
            "AND (po.pos_order_created_at IS NULL OR po.pos_order_created_at BETWEEN :startDate AND :endDate) " +
            "GROUP BY COALESCE(e.employee_id, e2.employee_id, ps.pos_session_employee_id, 1), COALESCE(NULLIF(NULLIF(e.employee_fullname, ''), 'Ko có tên'), NULLIF(NULLIF(e2.employee_fullname, ''), 'Ko có tên'), NULLIF(u.username, ''), CONCAT('Nhân viên #', COALESCE(e.employee_id, e2.employee_id, ps.pos_session_employee_id, 1))) " +
            "ORDER BY total_revenue DESC", nativeQuery = true)
    List<Object[]> getTopPosStaff(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT " +
            "e.employee_id as employee_id, " +
            "COALESCE(NULLIF(NULLIF(e.employee_fullname, ''), 'Ko có tên'), NULLIF(u.username, ''), 'Nhân viên') as employee_name, " +
            "COUNT(DISTINCT o.order_id) as total_orders, " +
            "COALESCE(SUM(COALESCE(o.order_total_amount, 0)), 0) as total_revenue " +
            "FROM orders o " +
            "JOIN users u ON (LOWER(o.order_processed_by_email) = LOWER(u.email) OR LOWER(o.order_processed_by) = LOWER(u.username) OR LOWER(o.order_processed_by) = LOWER(u.email)) " +
            "JOIN employees e ON e.employee_user_id = u.user_id " +
            "WHERE o.order_status = 'COMPLETED' " +
            "AND o.order_deleted_at IS NULL " +
            "AND (o.order_created_at IS NULL OR o.order_created_at BETWEEN :startDate AND :endDate) " +
            "GROUP BY e.employee_id, COALESCE(NULLIF(NULLIF(e.employee_fullname, ''), 'Ko có tên'), NULLIF(u.username, ''), 'Nhân viên') " +
            "ORDER BY total_revenue DESC", nativeQuery = true)
    List<Object[]> getTopOnlineStaff(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    // =========================================
    // 8. TỔNG DOANH THU TÍCH LŨY (ALL TIME)
    // =========================================

    /**
     * Tổng doanh thu online từ trước đến nay (chỉ COMPLETED)
     */
    @Query("SELECT COALESCE(SUM(o.orderTotalAmount - CASE WHEN o.orderStatus = 'RETURNED' THEN COALESCE(o.refundAmount, 0) ELSE 0 END), 0) FROM Order o " +
            "WHERE o.orderStatus IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.deletedAt IS NULL")
    BigDecimal getTotalOnlineRevenueAllTime();

    /**
     * Tổng doanh thu POS từ trước đến nay
     */
    @Query(value = "SELECT COALESCE(SUM(COALESCE(NULLIF(po.pos_order_total_amount, 0), NULLIF(po.pos_order_paid_amount, 0), NULLIF(po.pos_order_subtotal - COALESCE(po.pos_order_discount_amount, 0), 0), (SELECT COALESCE(SUM(poi.pos_order_item_total), 0) FROM pos_order_items poi WHERE poi.pos_order_item_order_id = po.pos_order_id), 0)), 0) FROM pos_orders po " +
            "WHERE (po.pos_order_status IS NULL OR UPPER(po.pos_order_status) NOT IN ('CANCELLED', 'DELETED'))", nativeQuery = true)
    BigDecimal getTotalPosRevenueAllTime();

    /**
     * Tổng số đơn hàng online từ trước đến nay (chỉ COMPLETED)
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.orderStatus IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.deletedAt IS NULL")
    Long getTotalOnlineOrdersAllTime();

    /**
     * Tổng số đơn hàng POS từ trước đến nay (chỉ COMPLETED)
     */
    @Query(value = "SELECT COUNT(po.pos_order_id) FROM pos_orders po " +
            "WHERE po.pos_order_status = 'COMPLETED'", nativeQuery = true)
    Long getTotalPosOrdersAllTime();

    // =========================================
    // 9. ✅ THÊM MỚI: TỔNG DOANH THU TỪ TẤT CẢ ĐƠN HÀNG COMPLETED
    // =========================================

    /**
     * Tổng doanh thu từ tất cả đơn hàng COMPLETED (Online + POS)
     */
    @Query("SELECT COALESCE(SUM(o.orderTotalAmount - CASE WHEN o.orderStatus = 'RETURNED' THEN COALESCE(o.refundAmount, 0) ELSE 0 END), 0) FROM Order o " +
            "WHERE o.orderStatus IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.deletedAt IS NULL")
    BigDecimal getTotalRevenueFromAllOrders();

    /**
     * Tổng doanh thu từ tất cả đơn hàng COMPLETED theo khoảng thời gian
     */
    @Query("SELECT COALESCE(SUM(o.orderTotalAmount - CASE WHEN o.orderStatus = 'RETURNED' THEN COALESCE(o.refundAmount, 0) ELSE 0 END), 0) FROM Order o " +
            "WHERE o.orderStatus IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.deletedAt IS NULL " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRevenueFromAllOrdersBetween(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Tổng số đơn hàng COMPLETED từ tất cả
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.orderStatus IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.deletedAt IS NULL")
    Long getTotalCompletedOrdersAllTime();

    /**
     * Tổng số đơn hàng COMPLETED theo khoảng thời gian
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.orderStatus IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.deletedAt IS NULL " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    Long getTotalCompletedOrdersBetween(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Tổng doanh thu trung bình mỗi đơn hàng COMPLETED
     */
    @Query("SELECT COALESCE(AVG(o.orderTotalAmount - CASE WHEN o.orderStatus = 'RETURNED' THEN COALESCE(o.refundAmount, 0) ELSE 0 END), 0) FROM Order o " +
            "WHERE o.orderStatus IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.deletedAt IS NULL")
    BigDecimal getAverageOrderValueAllTime();

    /**
     * Tổng doanh thu trung bình mỗi đơn hàng COMPLETED theo khoảng thời gian
     */
    @Query("SELECT COALESCE(AVG(o.orderTotalAmount - CASE WHEN o.orderStatus = 'RETURNED' THEN COALESCE(o.refundAmount, 0) ELSE 0 END), 0) FROM Order o " +
            "WHERE o.orderStatus IN ('COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED') " +
            "AND o.deletedAt IS NULL " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getAverageOrderValueBetween(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
}