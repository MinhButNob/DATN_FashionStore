package com.example.backend_fashionstore.QuanLyBanner.repository;

import com.example.backend_fashionstore.entity.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}