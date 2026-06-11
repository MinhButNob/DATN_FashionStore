package com.example.backend_fashionstore.QuanLyDanhMuc.repository;

import com.example.backend_fashionstore.entity.Category;
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
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // Lấy tất cả chưa xóa
    @Query("SELECT c FROM Category c WHERE c.deletedAt IS NULL")
    List<Category> findAllActive();

    // Lấy theo ID (chưa xóa)
    @Query("SELECT c FROM Category c WHERE c.categoryId = :id AND c.deletedAt IS NULL")
    Optional<Category> findByCategoryIdAndDeletedAtIsNull(@Param("id") Integer id);

    // Tìm kiếm theo tên (chưa xóa)
    @Query("SELECT c FROM Category c WHERE LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND c.deletedAt IS NULL")
    Page<Category> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Kiểm tra tồn tại theo tên (chưa xóa)
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.categoryName = :name AND c.deletedAt IS NULL")
    boolean existsByCategoryNameAndDeletedAtIsNull(@Param("name") String name);

    // Xóa mềm
    @Modifying
    @Transactional
    @Query("UPDATE Category c SET c.deletedAt = :now WHERE c.categoryId = :id AND c.deletedAt IS NULL")
    int softDelete(@Param("id") Integer id, @Param("now") LocalDateTime now);

    // Khôi phục
    @Modifying
    @Transactional
    @Query("UPDATE Category c SET c.deletedAt = NULL WHERE c.categoryId = :id AND c.deletedAt IS NOT NULL")
    int restore(@Param("id") Integer id);

    // Đếm số lượng chưa xóa
    @Query("SELECT COUNT(c) FROM Category c WHERE c.deletedAt IS NULL")
    long countByDeletedAtIsNull();

    // Lấy danh sách đang ACTIVE (dùng cho dropdown)
    @Query("SELECT c FROM Category c WHERE c.status = 'ACTIVE' AND c.deletedAt IS NULL")
    List<Category> findActiveCategories();
}