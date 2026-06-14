package com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository;

import com.example.backend_fashionstore.entity.PosHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PosHoldRepository extends JpaRepository<PosHold, Integer> {

    Optional<PosHold> findByPosHoldReferenceCode(String referenceCode);

    List<PosHold> findByPosHoldSessionIdAndPosHoldStatus(Integer sessionId, String status);

    @Query("SELECT h FROM PosHold h WHERE h.posHoldExpiryTime < :now AND h.posHoldStatus = 'ACTIVE'")
    List<PosHold> findExpiredHolds(@Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE PosHold h SET h.posHoldStatus = 'EXPIRED' WHERE h.posHoldId IN " +
            "(SELECT h2.posHoldId FROM PosHold h2 WHERE h2.posHoldExpiryTime < :now AND h2.posHoldStatus = 'ACTIVE')")
    void markExpiredHolds(@Param("now") LocalDateTime now);
}