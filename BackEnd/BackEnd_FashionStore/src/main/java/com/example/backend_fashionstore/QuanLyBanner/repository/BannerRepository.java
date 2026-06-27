package com.example.backend_fashionstore.QuanLyBanner.repository;

import com.example.backend_fashionstore.entity.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Integer> {

    // Lấy tất cả banner không bị xóa, sắp xếp theo displayOrder
    List<Banner> findByStatusNotOrderByDisplayOrderAsc(String status);

    // Lấy banner đang ACTIVE để hiển thị trên trang chủ
    List<Banner> findByStatusOrderByDisplayOrderAsc(String status);

    // Tìm kiếm theo title (admin)
    @Query("SELECT b FROM Banner b WHERE b.status != 'DELETED' AND " +
            "(:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Banner> searchBanners(@Param("keyword") String keyword, Pageable pageable);

    // Đếm banner theo trạng thái
    long countByStatus(String status);

    // ========== KIỂM TRA THỨ TỰ ==========

    /**
     * Kiểm tra xem displayOrder đã tồn tại chưa (không tính banner đã xóa)
     */
    @Query("SELECT COUNT(b) > 0 FROM Banner b WHERE b.displayOrder = :displayOrder AND b.status != 'DELETED'")
    boolean existsByDisplayOrderAndNotDeleted(@Param("displayOrder") Integer displayOrder);

    /**
     * Kiểm tra xem displayOrder đã tồn tại chưa (không tính banner đã xóa và không tính banner đang sửa)
     */
    @Query("SELECT COUNT(b) > 0 FROM Banner b WHERE b.displayOrder = :displayOrder AND b.status != 'DELETED' AND b.bannerId != :excludeId")
    boolean existsByDisplayOrderAndNotDeletedAndIdNot(@Param("displayOrder") Integer displayOrder, @Param("excludeId") Integer excludeId);

    /**
     * Lấy displayOrder lớn nhất hiện tại (không tính banner đã xóa)
     */
    @Query("SELECT MAX(b.displayOrder) FROM Banner b WHERE b.status != 'DELETED'")
    Integer findMaxDisplayOrder();

    /**
     * Tìm banner theo displayOrder (không tính đã xóa)
     */
    @Query("SELECT b FROM Banner b WHERE b.displayOrder = :displayOrder AND b.status != 'DELETED'")
    Optional<Banner> findByDisplayOrderAndNotDeleted(@Param("displayOrder") Integer displayOrder);

    /**
     * Giảm displayOrder của các banner có thứ tự lớn hơn (khi xóa banner)
     */
    @Modifying
    @Query("UPDATE Banner b SET b.displayOrder = b.displayOrder - 1 " +
            "WHERE b.displayOrder > :order AND b.status != 'DELETED'")
    void decrementDisplayOrderAfter(@Param("order") Integer order);
}