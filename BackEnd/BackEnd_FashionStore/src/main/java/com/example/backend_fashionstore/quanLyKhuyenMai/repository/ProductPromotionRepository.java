package com.example.backend_fashionstore.quanLyKhuyenMai.repository;

import com.example.backend_fashionstore.entity.ProductPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProductPromotionRepository extends JpaRepository<ProductPromotion, Integer> {

    // ✅ SỬA: Dùng @Query thay vì method name
    @Query("SELECT pp FROM ProductPromotion pp WHERE pp.productDetail.productDetailId = :productDetailId")
    List<ProductPromotion> findByProductDetailId(@Param("productDetailId") Integer productDetailId);

    // ✅ Xóa theo product detail id
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductPromotion pp WHERE pp.productDetail.productDetailId = :productDetailId")
    void deleteByProductDetailId(@Param("productDetailId") Integer productDetailId);

    // ✅ Kiểm tra tồn tại
    @Query("SELECT COUNT(pp) > 0 FROM ProductPromotion pp WHERE pp.productDetail.productDetailId = :productDetailId AND pp.promotion.promotionId = :promotionId")
    boolean existsByProductDetailIdAndPromotionId(@Param("productDetailId") Integer productDetailId, @Param("promotionId") Integer promotionId);

    // Thêm vào ProductPromotionRepository.java
    @Query("SELECT pp FROM ProductPromotion pp WHERE pp.productDetail.productDetailId IN :detailIds")
    List<ProductPromotion> findAllByProductDetailIds(@Param("detailIds") List<Integer> detailIds);
}