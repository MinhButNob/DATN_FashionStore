// quanLyBanhangTaiQuay/repository/PosOrderRepository.java
package com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository;

import com.example.backend_fashionstore.entity.PosOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PosOrderRepository extends JpaRepository<PosOrder, Integer> {

    Optional<PosOrder> findByPosOrderInvoiceNumber(String invoiceNumber);

    List<PosOrder> findByPosOrderSessionId(Integer sessionId);

    List<PosOrder> findByPosOrderSessionIdAndPosOrderStatus(Integer sessionId, String status);

    // ========== THỐNG KÊ DOANH THU ==========

    @Query("SELECT COALESCE(SUM(o.posOrderTotalAmount), 0) FROM PosOrder o " +
            "WHERE o.posOrderSessionId = :sessionId AND o.posOrderStatus = 'COMPLETED'")
    BigDecimal getTotalSalesBySession(@Param("sessionId") Integer sessionId);

    @Query("SELECT COUNT(o) FROM PosOrder o WHERE o.posOrderSessionId = :sessionId AND o.posOrderStatus = 'COMPLETED'")
    Integer getOrderCountBySession(@Param("sessionId") Integer sessionId);

    @Query("SELECT o FROM PosOrder o WHERE o.posOrderCreatedAt BETWEEN :startDate AND :endDate")
    List<PosOrder> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // ========== THỐNG KÊ TỔNG HỢP CHO QUẢN LÝ HÓA ĐƠN ==========

    /**
     * Tổng doanh thu trong khoảng thời gian
     */
    @Query("SELECT COALESCE(SUM(o.posOrderTotalAmount), 0) FROM PosOrder o " +
            "WHERE o.posOrderCreatedAt BETWEEN :startDate AND :endDate " +
            "AND o.posOrderStatus = 'COMPLETED'")
    BigDecimal getTotalRevenue(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    /**
     * Tổng số đơn hàng hoàn thành trong khoảng thời gian
     */
    @Query("SELECT COUNT(o) FROM PosOrder o " +
            "WHERE o.posOrderCreatedAt BETWEEN :startDate AND :endDate " +
            "AND o.posOrderStatus = 'COMPLETED'")
    Long getTotalOrders(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

    /**
     * Số lượng khách hàng duy nhất (có customer_id) trong khoảng thời gian
     */
    @Query("SELECT COUNT(DISTINCT o.posOrderCustomerId) FROM PosOrder o " +
            "WHERE o.posOrderCreatedAt BETWEEN :startDate AND :endDate " +
            "AND o.posOrderCustomerId IS NOT NULL " +
            "AND o.posOrderStatus = 'COMPLETED'")
    Long getTotalCustomers(@Param("startDate") LocalDateTime startDate,
                           @Param("endDate") LocalDateTime endDate);

    /**
     * Tổng số tiền giảm giá trong khoảng thời gian
     */
    @Query("SELECT COALESCE(SUM(o.posOrderDiscountAmount), 0) FROM PosOrder o " +
            "WHERE o.posOrderCreatedAt BETWEEN :startDate AND :endDate " +
            "AND o.posOrderStatus = 'COMPLETED'")
    BigDecimal getTotalDiscount(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    /**
     * Tổng số tiền thuế trong khoảng thời gian
     */
    @Query("SELECT COALESCE(SUM(o.posOrderTaxAmount), 0) FROM PosOrder o " +
            "WHERE o.posOrderCreatedAt BETWEEN :startDate AND :endDate " +
            "AND o.posOrderStatus = 'COMPLETED'")
    BigDecimal getTotalTax(@Param("startDate") LocalDateTime startDate,
                           @Param("endDate") LocalDateTime endDate);

    // ========== TÌM KIẾM HÓA ĐƠN (PHÂN TRANG) ==========

    // ========== TÌM KIẾM HÓA ĐƠN (PHÂN TRANG - ĐÃ CẬP NHẬT CHUẨN) ==========

    @Query("SELECT o FROM PosOrder o WHERE " +
            "(:invoiceNumber IS NULL OR LOWER(o.posOrderInvoiceNumber) LIKE LOWER(CONCAT('%', CAST(:invoiceNumber AS text), '%'))) AND " +
            "(:customerName IS NULL OR LOWER(o.posOrderCustomerName) LIKE LOWER(CONCAT('%', CAST(:customerName AS text), '%'))) AND " +
            "(:customerPhone IS NULL OR o.posOrderCustomerPhone LIKE CONCAT('%', CAST(:customerPhone AS text), '%')) AND " +
            "(:orderStatus IS NULL OR o.posOrderStatus = :orderStatus) AND " +
            "(:paymentStatus IS NULL OR o.posOrderPaymentStatus = :paymentStatus) AND " +
            "(CAST(:posSessionId AS integer) IS NULL OR o.posOrderSessionId = :posSessionId) AND " +
            "(o.posOrderCreatedAt >= :startDate AND o.posOrderCreatedAt <= :endDate) AND " +
            "(o.posOrderDeletedAt IS NULL)")
    Page<PosOrder> searchInvoices(@Param("invoiceNumber") String invoiceNumber,
                                  @Param("customerName") String customerName,
                                  @Param("customerPhone") String customerPhone,
                                  @Param("orderStatus") String orderStatus,
                                  @Param("paymentStatus") String paymentStatus,
                                  @Param("posSessionId") Integer posSessionId,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate,
                                  Pageable pageable);
}