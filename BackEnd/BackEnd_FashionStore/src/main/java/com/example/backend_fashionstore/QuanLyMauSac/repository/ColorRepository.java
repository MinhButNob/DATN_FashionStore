package com.example.backend_fashionstore.QuanLyMauSac.repository;

import com.example.backend_fashionstore.entity.Color;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface ColorRepository extends JpaRepository<Color, Integer> {

    // Xóa mềm
    @Modifying
    @Transactional
    @Query("UPDATE Color c SET c.deletedAt = :now WHERE c.colorId = :id AND c.deletedAt IS NULL")
    int softDelete(@Param("id") Integer id, @Param("now") LocalDateTime now);

    // Khôi phục
    @Modifying
    @Transactional
    @Query("UPDATE Color c SET c.deletedAt = NULL WHERE c.colorId = :id AND c.deletedAt IS NOT NULL")
    int restore(@Param("id") Integer id);

    // Kiểm tra tồn tại theo mã màu (chưa xóa)
    @Query("SELECT COUNT(c) > 0 FROM Color c WHERE c.colorCode = :code AND c.deletedAt IS NULL")
    boolean existsByColorCodeAndDeletedAtIsNull(@Param("code") String code);

    // Kiểm tra tồn tại theo tên màu (chưa xóa)
    @Query("SELECT COUNT(c) > 0 FROM Color c WHERE LOWER(c.colorName) = LOWER(:name) AND c.deletedAt IS NULL")
    boolean existsByColorNameAndDeletedAtIsNull(@Param("name") String name);

    // Kiểm tra tồn tại theo mã HEX (chưa xóa)
    @Query("SELECT COUNT(c) > 0 FROM Color c WHERE LOWER(c.hexCode) = LOWER(:hex) AND c.deletedAt IS NULL")
    boolean existsByHexCodeAndDeletedAtIsNull(@Param("hex") String hex);

    // Tìm kiếm theo mã hoặc tên (phân trang, chưa xóa)
    @Query("SELECT c FROM Color c WHERE (LOWER(c.colorCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.colorName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND c.deletedAt IS NULL")
    Page<Color> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Lấy tất cả chưa xóa (không phân trang)
    @Query("SELECT c FROM Color c WHERE c.deletedAt IS NULL")
    List<Color> findAllActive();

    // Lấy tất cả chưa xóa (có phân trang)
    @Query("SELECT c FROM Color c WHERE c.deletedAt IS NULL")
    Page<Color> findAllActive(Pageable pageable);

    // Lấy theo ID (chưa xóa)
    @Query("SELECT c FROM Color c WHERE c.colorId = :id AND c.deletedAt IS NULL")
    Optional<Color> findByColorIdAndDeletedAtIsNull(@Param("id") Integer id);

    // Đếm số lượng chưa xóa
    @Query("SELECT COUNT(c) FROM Color c WHERE c.deletedAt IS NULL")
    long countByDeletedAtIsNull();

    // Lấy danh sách đang ACTIVE và chưa xóa (dùng cho dropdown)
    @Query("SELECT c FROM Color c WHERE c.status = 'ACTIVE' AND c.deletedAt IS NULL")
    List<Color> findActiveColors();
}