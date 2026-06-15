package com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository;

import com.example.backend_fashionstore.entity.PosPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PosPaymentRepository extends JpaRepository<PosPayment, Integer> {

    List<PosPayment> findByPosPaymentOrderId(Integer orderId);

    @Query("SELECT COALESCE(SUM(p.posPaymentAmount), 0) FROM PosPayment p " +
            "WHERE p.posPaymentOrderId IN (SELECT o.posOrderId FROM PosOrder o WHERE o.posOrderSessionId = :sessionId) " +
            "AND p.posPaymentStatus = 'COMPLETED'")
    BigDecimal getTotalPaymentBySession(@Param("sessionId") Integer sessionId);
}