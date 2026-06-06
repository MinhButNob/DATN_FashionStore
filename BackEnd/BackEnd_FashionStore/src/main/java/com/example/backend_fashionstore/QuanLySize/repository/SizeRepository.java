package com.example.backend_fashionstore.QuanLySize.repository;

import com.example.backend_fashionstore.entity.Size;
import com.example.backend_fashionstore.enums.SizeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SizeRepository extends JpaRepository<Size, Integer> {

    boolean existsBySizeCode(String sizeCode);

    // Tìm kiếm chỉ lấy size chưa xóa
    @Query("SELECT s FROM Size s WHERE s.status <> 'DELETED' AND " +
            "(LOWER(s.sizeCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.sizeName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Size> searchActive(@Param("keyword") String keyword, Pageable pageable);

    // Lấy tất cả size chưa xóa
    @Query("SELECT s FROM Size s WHERE s.status <> 'DELETED'")
    Page<Size> findAllActive(Pageable pageable);

    // Lấy size theo status
    Page<Size> findByStatus(SizeStatus status, Pageable pageable);

    // Đếm size chưa xóa
    @Query("SELECT COUNT(s) FROM Size s WHERE s.status <> 'DELETED'")
    long countActive();
}