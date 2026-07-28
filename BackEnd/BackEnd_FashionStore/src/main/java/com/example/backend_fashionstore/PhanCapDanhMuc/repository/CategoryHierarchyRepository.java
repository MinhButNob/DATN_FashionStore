package com.example.backend_fashionstore.PhanCapDanhMuc.repository;

import com.example.backend_fashionstore.entity.Category;
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
public interface CategoryHierarchyRepository extends JpaRepository<Category, Integer> {

    // Lấy theo ID (chưa xóa) - dùng khi validate cha/con
    @Query("SELECT c FROM Category c WHERE c.categoryId = :id AND c.deletedAt IS NULL")
    Optional<Category> findByCategoryIdAndDeletedAtIsNull(@Param("id") Integer id);

    // Danh mục cấp 1 (cha), chưa xóa - mọi trạng thái - dùng cho trang ADMIN
    @Query("SELECT c FROM Category c WHERE c.parentCategoryId IS NULL AND c.deletedAt IS NULL")
    List<Category> findParentCategories();

    // Danh mục cấp 1 (cha), chưa xóa và đang ACTIVE - dùng cho menu KHÁCH HÀNG
    @Query("SELECT c FROM Category c WHERE c.parentCategoryId IS NULL AND c.deletedAt IS NULL AND c.status = 'ACTIVE'")
    List<Category> findActiveParentCategories();

    // Danh mục con theo cha, chưa xóa - mọi trạng thái - dùng cho trang ADMIN
    @Query("SELECT c FROM Category c WHERE c.parentCategoryId = :parentId AND c.deletedAt IS NULL")
    List<Category> findChildren(@Param("parentId") Integer parentId);

    // Danh mục con theo cha, chưa xóa và đang ACTIVE - dùng cho menu KHÁCH HÀNG
    @Query("SELECT c FROM Category c WHERE c.parentCategoryId = :parentId AND c.deletedAt IS NULL AND c.status = 'ACTIVE'")
    List<Category> findActiveChildren(@Param("parentId") Integer parentId);

    // Kiểm tra 1 danh mục có đang là cha của danh mục con nào chưa bị xóa không
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.parentCategoryId = :parentId AND c.deletedAt IS NULL")
    boolean existsActiveChildren(@Param("parentId") Integer parentId);

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
}