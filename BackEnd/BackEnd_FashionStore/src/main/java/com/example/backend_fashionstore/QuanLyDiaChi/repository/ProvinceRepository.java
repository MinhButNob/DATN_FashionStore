package com.example.backend_fashionstore.QuanLyDiaChi.repository;

import com.example.backend_fashionstore.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Integer> {

    @Query("SELECT p FROM Province p WHERE p.status = 'ACTIVE' AND p.deletedAt IS NULL ORDER BY p.displayOrder ASC, p.provinceName ASC")
    List<Province> findAllActive();

    @Query("SELECT p FROM Province p WHERE p.deletedAt IS NULL ORDER BY p.displayOrder ASC, p.provinceName ASC")
    List<Province> findAllWithDeletedAtNull();

    Optional<Province> findByProvinceCodeAndDeletedAtIsNull(String provinceCode);

    boolean existsByProvinceCodeAndDeletedAtIsNull(String provinceCode);

    @Query("SELECT p FROM Province p WHERE p.deletedAt IS NULL AND LOWER(p.provinceName) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.displayOrder ASC")
    List<Province> searchByName(@Param("keyword") String keyword);
}