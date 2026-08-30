package com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository;

import com.example.backend_fashionstore.entity.DamagedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DamagedProductRepository extends JpaRepository<DamagedProduct, Integer> {

    List<DamagedProduct> findByReturnExchangeId(Integer returnExchangeId);

    List<DamagedProduct> findByStatus(String status);

    List<DamagedProduct> findByProductDetailId(Integer productDetailId);

    @Query("SELECT d FROM DamagedProduct d WHERE d.status = 'PENDING' ORDER BY d.createdAt DESC")
    List<DamagedProduct> findPendingDamagedProducts();

    @Query("SELECT COALESCE(SUM(d.quantity), 0) FROM DamagedProduct d WHERE d.productDetailId = :productDetailId AND d.status = 'PENDING'")
    Integer getTotalPendingDamagedQuantity(@Param("productDetailId") Integer productDetailId);

    @Query("SELECT COALESCE(SUM(d.quantity), 0) FROM DamagedProduct d WHERE d.productDetailId = :productDetailId")
    Integer getTotalDamagedQuantity(@Param("productDetailId") Integer productDetailId);

    @Modifying
    @Transactional
    @Query("UPDATE DamagedProduct d SET d.status = :status, d.processedAt = :processedAt, d.processedBy = :processedBy WHERE d.damagedProductId = :damagedProductId")
    int updateStatus(@Param("damagedProductId") Integer damagedProductId,
                     @Param("status") String status,
                     @Param("processedAt") LocalDateTime processedAt,
                     @Param("processedBy") Integer processedBy);

    @Query("SELECT d FROM DamagedProduct d WHERE d.status = :status ORDER BY d.createdAt DESC")
    List<DamagedProduct> findByStatusOrderByCreatedAtDesc(@Param("status") String status);

    @Query("SELECT COUNT(d) FROM DamagedProduct d WHERE d.status = 'PENDING'")
    Long countPendingDamagedProducts();
}