package com.example.backend_fashionstore.QuanLyDiaChi.repository;

import com.example.backend_fashionstore.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WardRepository extends JpaRepository<Ward, Integer> {

    @Query("SELECT w FROM Ward w WHERE w.province.provinceId = :provinceId AND w.status = 'ACTIVE' AND w.deletedAt IS NULL ORDER BY w.displayOrder ASC, w.wardName ASC")
    List<Ward> findByProvinceIdAndStatusActive(@Param("provinceId") Integer provinceId);

    @Query("SELECT w FROM Ward w WHERE w.province.provinceId = :provinceId AND w.deletedAt IS NULL ORDER BY w.displayOrder ASC, w.wardName ASC")
    List<Ward> findByProvinceId(@Param("provinceId") Integer provinceId);

    // ✅ THÊM MỚI: Lấy tất cả phường/xã chưa bị xóa
    @Query("SELECT w FROM Ward w WHERE w.deletedAt IS NULL ORDER BY w.displayOrder ASC, w.wardName ASC")
    List<Ward> findAllWithDeletedAtNull();

    @Query("SELECT w FROM Ward w WHERE w.deletedAt IS NULL AND LOWER(w.wardName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Ward> searchByName(@Param("keyword") String keyword);

    Optional<Ward> findByWardCodeAndDeletedAtIsNull(String wardCode);

    boolean existsByWardCodeAndDeletedAtIsNull(String wardCode);
}