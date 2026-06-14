// quanLyBanhangTaiQuay/repository/PosOrderRepository.java
package com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository;

import com.example.backend_fashionstore.entity.PosOrder;
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

    // Đảm bảo method trả về BigDecimal không null
    @Query("SELECT COALESCE(SUM(o.posOrderTotalAmount), 0) FROM PosOrder o " +
            "WHERE o.posOrderSessionId = :sessionId AND o.posOrderStatus = 'COMPLETED'")
    BigDecimal getTotalSalesBySession(@Param("sessionId") Integer sessionId);

    @Query("SELECT COUNT(o) FROM PosOrder o WHERE o.posOrderSessionId = :sessionId AND o.posOrderStatus = 'COMPLETED'")
    Integer getOrderCountBySession(@Param("sessionId") Integer sessionId);

    @Query("SELECT o FROM PosOrder o WHERE o.posOrderCreatedAt BETWEEN :startDate AND :endDate")
    List<PosOrder> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
}