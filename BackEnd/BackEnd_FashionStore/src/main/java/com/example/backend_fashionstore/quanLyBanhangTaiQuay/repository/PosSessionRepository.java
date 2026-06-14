package com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository;

import com.example.backend_fashionstore.entity.PosSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PosSessionRepository extends JpaRepository<PosSession, Integer> {

    Optional<PosSession> findByPosSessionCashRegisterIdAndPosSessionStatus(Integer cashRegisterId, String status);

    List<PosSession> findByPosSessionEmployeeIdAndPosSessionStatus(Integer employeeId, String status);

    @Query("SELECT s FROM PosSession s WHERE s.posSessionStatus = 'ACTIVE'")
    List<PosSession> findAllActiveSessions();

    @Query("SELECT s FROM PosSession s WHERE s.posSessionEmployeeId = :employeeId AND s.posSessionStatus = 'ACTIVE'")
    Optional<PosSession> findActiveSessionByEmployee(@Param("employeeId") Integer employeeId);
}
