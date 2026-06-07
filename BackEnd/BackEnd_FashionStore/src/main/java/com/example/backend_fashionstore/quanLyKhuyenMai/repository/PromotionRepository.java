package com.example.backend_fashionstore.quanLyKhuyenMai.repository;

import com.example.backend_fashionstore.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    // Lấy tất cả (chưa xóa)
    @Query("SELECT p FROM Promotion p WHERE p.promotionDeletedAt IS NULL")
    Page<Promotion> findAllActive(Pageable pageable);

    // Lấy danh sách đã xóa
    @Query("SELECT p FROM Promotion p WHERE p.promotionDeletedAt IS NOT NULL")
    Page<Promotion> findDeletedPromotions(Pageable pageable);

    // Tìm kiếm theo tên (chưa xóa)
    @Query("SELECT p FROM Promotion p WHERE " +
            "LOWER(p.promotionName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND p.promotionDeletedAt IS NULL")
    Page<Promotion> searchByPromotionName(@Param("keyword") String keyword, Pageable pageable);

    // Lọc theo trạng thái (chưa xóa)
    @Query("SELECT p FROM Promotion p WHERE " +
            "p.promotionStatus = :status AND p.promotionDeletedAt IS NULL")
    Page<Promotion> findByPromotionStatus(@Param("status") String status, Pageable pageable);

    // Lọc + tìm kiếm (chưa xóa)
    @Query("SELECT p FROM Promotion p WHERE " +
            "p.promotionStatus = :status AND " +
            "LOWER(p.promotionName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " +
            "p.promotionDeletedAt IS NULL")
    Page<Promotion> searchByPromotionStatusAndName(
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable);

    // Thống kê tổng số (chưa xóa)
    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.promotionDeletedAt IS NULL")
    long countAllActive();

    // Thống kê số đang hoạt động (chưa xóa)
    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.promotionStatus = 'ACTIVE' AND p.promotionDeletedAt IS NULL")
    long countByStatusActive();

    // Thống kê số đã dừng (chưa xóa)
    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.promotionStatus = 'INACTIVE' AND p.promotionDeletedAt IS NULL")
    long countByStatusInactive();

    // Kiểm tra tên trùng (chưa xóa)
    @Query("SELECT COUNT(p) > 0 FROM Promotion p WHERE " +
            "LOWER(p.promotionName) = LOWER(:name) AND p.promotionDeletedAt IS NULL")
    boolean existsByPromotionName(@Param("name") String name);

    // Kiểm tra tên trùng khi sửa (chưa xóa)
    @Query("SELECT COUNT(p) > 0 FROM Promotion p WHERE " +
            "LOWER(p.promotionName) = LOWER(:name) AND " +
            "p.promotionId != :id AND p.promotionDeletedAt IS NULL")
    boolean existsByPromotionNameAndIdNot(@Param("name") String name, @Param("id") Integer id);
}