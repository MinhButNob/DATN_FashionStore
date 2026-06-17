package com.example.backend_fashionstore.quanLyHoaDonPos.repository;

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
public interface PosInvoiceRepository extends JpaRepository<PosOrder, Integer> {

        Optional<PosOrder> findByPosOrderInvoiceNumber(String invoiceNumber);

        // Native Query - Đã bổ sung lọc theo posSessionId linh hoạt
        @Query(value = "SELECT * FROM pos_orders o WHERE " +
                "(:invoiceNumber IS NULL OR o.pos_order_invoice_number LIKE CONCAT('%', :invoiceNumber, '%')) AND " +
                "(:customerName IS NULL OR o.pos_order_customer_name LIKE CONCAT('%', :customerName, '%')) AND " +
                "(:customerPhone IS NULL OR o.pos_order_customer_phone LIKE CONCAT('%', :customerPhone, '%')) AND " +
                "(:orderStatus IS NULL OR o.pos_order_status = :orderStatus) AND " +
                "(:paymentStatus IS NULL OR o.pos_order_payment_status = :paymentStatus) AND " +
                "(:posSessionId IS NULL OR o.pos_order_session_id = :posSessionId) AND " +
                "(o.pos_order_created_at >= :startDate AND o.pos_order_created_at <= :endDate) AND " + // Thay BETWEEN bằng so sánh trực tiếp
                "(o.pos_order_deleted_at IS NULL)", // Thử tạm thời xóa dòng này nếu test vẫn chưa ra dữ liệu
                countQuery = "SELECT COUNT(*) FROM pos_orders o WHERE " +
                        "(:invoiceNumber IS NULL OR o.pos_order_invoice_number LIKE CONCAT('%', :invoiceNumber, '%')) AND " +
                        "(:customerName IS NULL OR o.pos_order_customer_name LIKE CONCAT('%', :customerName, '%')) AND " +
                        "(:customerPhone IS NULL OR o.pos_order_customer_phone LIKE CONCAT('%', :customerPhone, '%')) AND " +
                        "(:orderStatus IS NULL OR o.pos_order_status = :orderStatus) AND " +
                        "(:paymentStatus IS NULL OR o.pos_order_payment_status = :paymentStatus) AND " +
                        "(:posSessionId IS NULL OR o.pos_order_session_id = :posSessionId) AND " +
                        "(o.pos_order_created_at >= :startDate AND o.pos_order_created_at <= :endDate) AND " +
                        "(o.pos_order_deleted_at IS NULL)",
                nativeQuery = true)
        Page<PosOrder> searchInvoicesNative(@Param("invoiceNumber") String invoiceNumber,
                                            @Param("customerName") String customerName,
                                            @Param("customerPhone") String customerPhone,
                                            @Param("orderStatus") String orderStatus,
                                            @Param("paymentStatus") String paymentStatus,
                                            @Param("posSessionId") Integer posSessionId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            Pageable pageable);

        @Query(value = "SELECT COALESCE(SUM(pos_order_total_amount), 0) FROM pos_orders " +
                "WHERE pos_order_created_at BETWEEN :startDate AND :endDate " +
                "AND pos_order_status = 'COMPLETED'", nativeQuery = true)
        BigDecimal getTotalRevenue(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

        @Query(value = "SELECT COUNT(*) FROM pos_orders " +
                "WHERE pos_order_created_at BETWEEN :startDate AND :endDate " +
                "AND pos_order_status = 'COMPLETED'", nativeQuery = true)
        Long getTotalOrders(@Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate);

        @Query(value = "SELECT COUNT(DISTINCT pos_order_customer_id) FROM pos_orders " +
                "WHERE pos_order_created_at BETWEEN :startDate AND :endDate " +
                "AND pos_order_customer_id IS NOT NULL", nativeQuery = true)
        Long getTotalCustomers(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);
}