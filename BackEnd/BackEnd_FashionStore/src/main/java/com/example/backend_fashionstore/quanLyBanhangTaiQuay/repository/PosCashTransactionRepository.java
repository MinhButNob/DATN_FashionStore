package com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository;

import com.example.backend_fashionstore.entity.PosCashTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PosCashTransactionRepository extends JpaRepository<PosCashTransaction, Integer> {

    List<PosCashTransaction> findByCashTransactionSessionId(Integer sessionId);

    @Query("SELECT COALESCE(SUM(CASE WHEN t.cashTransactionType = 'DEPOSIT' THEN t.cashTransactionAmount ELSE 0 END), 0) - " +
            "COALESCE(SUM(CASE WHEN t.cashTransactionType = 'WITHDRAW' THEN t.cashTransactionAmount ELSE 0 END), 0) " +
            "FROM PosCashTransaction t WHERE t.cashTransactionSessionId = :sessionId")
    BigDecimal getNetCashMovement(@Param("sessionId") Integer sessionId);
}