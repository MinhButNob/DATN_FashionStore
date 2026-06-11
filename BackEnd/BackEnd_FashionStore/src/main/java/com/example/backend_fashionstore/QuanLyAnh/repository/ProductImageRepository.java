package com.example.backend_fashionstore.QuanLyAnh.repository;

import com.example.backend_fashionstore.entity.ProductImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {

    // ✅ Tìm theo product_detail_id (FETCH JOIN để lấy product, color, size)
    @Query("SELECT pi FROM ProductImage pi " +
            "LEFT JOIN FETCH pi.productDetail pd " +
            "LEFT JOIN FETCH pd.product p " +
            "LEFT JOIN FETCH pd.color c " +
            "LEFT JOIN FETCH pd.size s " +
            "WHERE pd.productDetailId = :detailId AND pi.deletedAt IS NULL AND pi.status = 'ACTIVE'")
    List<ProductImage> findByProductDetailId(@Param("detailId") Integer detailId);

    // ✅ Tìm theo product_detail_id có phân trang (FETCH JOIN)
    @Query("SELECT pi FROM ProductImage pi " +
            "LEFT JOIN FETCH pi.productDetail pd " +
            "LEFT JOIN FETCH pd.product p " +
            "LEFT JOIN FETCH pd.color c " +
            "LEFT JOIN FETCH pd.size s " +
            "WHERE pd.productDetailId = :detailId AND pi.deletedAt IS NULL AND pi.status = 'ACTIVE'")
    Page<ProductImage> findByProductDetail_ProductDetailId(@Param("detailId") Integer detailId, Pageable pageable);

    // ✅ Tìm theo đường dẫn ảnh (search)
    @Query("SELECT pi FROM ProductImage pi " +
            "LEFT JOIN FETCH pi.productDetail pd " +
            "LEFT JOIN FETCH pd.product p " +
            "WHERE LOWER(pi.productImagePath) LIKE LOWER(CONCAT('%', :keyword, '%')) AND pi.deletedAt IS NULL AND pi.status = 'ACTIVE'")
    Page<ProductImage> findByProductImagePathContaining(@Param("keyword") String keyword, Pageable pageable);

    // ✅ Đếm theo product_detail_id
    @Query("SELECT COUNT(pi) FROM ProductImage pi WHERE pi.productDetail.productDetailId = :detailId AND pi.deletedAt IS NULL AND pi.status = 'ACTIVE'")
    long countByProductDetail_ProductDetailId(@Param("detailId") Integer detailId);

    // ✅ Lấy display_order lớn nhất
    @Query("SELECT COALESCE(MAX(pi.displayOrder), 0) FROM ProductImage pi WHERE pi.productDetail.productDetailId = :detailId AND pi.deletedAt IS NULL AND pi.status = 'ACTIVE'")
    int getMaxDisplayOrderByProductDetailId(@Param("detailId") Integer detailId);

    // ✅ Lấy tất cả ảnh của product_detail sắp xếp theo display_order
    @Query("SELECT pi FROM ProductImage pi " +
            "LEFT JOIN FETCH pi.productDetail pd " +
            "LEFT JOIN FETCH pd.product p " +
            "WHERE pd.productDetailId = :detailId AND pi.deletedAt IS NULL AND pi.status = 'ACTIVE' ORDER BY pi.displayOrder ASC")
    List<ProductImage> findAllByProductDetailIdOrderByDisplayOrder(@Param("detailId") Integer detailId);

    // ✅ Tìm ảnh chính
    @Query("SELECT pi FROM ProductImage pi " +
            "LEFT JOIN FETCH pi.productDetail pd " +
            "LEFT JOIN FETCH pd.product p " +
            "WHERE pd.productDetailId = :detailId AND pi.isMain = true AND pi.deletedAt IS NULL AND pi.status = 'ACTIVE'")
    Optional<ProductImage> findMainImageByProductDetailId(@Param("detailId") Integer detailId);

    // ✅ Soft delete
    @Modifying
    @Transactional
    @Query("UPDATE ProductImage pi SET pi.status = 'DELETED', pi.deletedAt = CURRENT_TIMESTAMP WHERE pi.productImageId = :id AND pi.deletedAt IS NULL")
    int softDelete(@Param("id") Integer id);

    // ✅ Khôi phục ảnh đã xóa mềm
    @Modifying
    @Transactional
    @Query("UPDATE ProductImage pi SET pi.status = 'ACTIVE', pi.deletedAt = NULL WHERE pi.productImageId = :id AND pi.deletedAt IS NOT NULL")
    int restore(@Param("id") Integer id);

    // ✅ Lấy tất cả ảnh đã xóa mềm
    @Query("SELECT pi FROM ProductImage pi WHERE pi.deletedAt IS NOT NULL")
    List<ProductImage> findAllDeleted();

    // ✅ Xóa cứng theo product_detail_id (cleanup)
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductImage pi WHERE pi.productDetail.productDetailId = :detailId AND pi.deletedAt IS NOT NULL")
    void hardDeleteByProductDetailId(@Param("detailId") Integer detailId);
}