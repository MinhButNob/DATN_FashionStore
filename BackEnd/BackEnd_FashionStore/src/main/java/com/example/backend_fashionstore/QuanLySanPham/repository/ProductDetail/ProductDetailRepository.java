package com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail;

import com.example.backend_fashionstore.entity.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductDetailRepository extends JpaRepository<ProductDetail, Integer>, JpaSpecificationExecutor<ProductDetail> {

    @Query("SELECT pd FROM ProductDetail pd WHERE pd.sku = :sku AND pd.deletedAt IS NULL")
    Optional<ProductDetail> findBySku(@Param("sku") String sku);

    @Query("SELECT COUNT(pd) > 0 FROM ProductDetail pd WHERE pd.sku = :sku AND pd.deletedAt IS NULL")
    boolean existsBySku(@Param("sku") String sku);

    @Query("SELECT pd FROM ProductDetail pd WHERE pd.product.productId = :productId AND pd.deletedAt IS NULL")
    List<ProductDetail> findByProductId(@Param("productId") Integer productId);

    @Query("SELECT pd FROM ProductDetail pd WHERE pd.product.productId = :productId AND pd.color.colorId = :colorId AND pd.size.sizeId = :sizeId AND pd.deletedAt IS NULL")
    Optional<ProductDetail> findByProductIdAndColorIdAndSizeId(
            @Param("productId") Integer productId,
            @Param("colorId") Integer colorId,
            @Param("sizeId") Integer sizeId
    );

    @Query("SELECT MIN(pd.price) FROM ProductDetail pd WHERE pd.product.productId = :productId AND pd.deletedAt IS NULL")
    BigDecimal getMinPriceByProductId(@Param("productId") Integer productId);

    @Query("SELECT MAX(pd.price) FROM ProductDetail pd WHERE pd.product.productId = :productId AND pd.deletedAt IS NULL")
    BigDecimal getMaxPriceByProductId(@Param("productId") Integer productId);

    @Modifying
    @Transactional
    @Query("UPDATE ProductDetail pd SET pd.deletedAt = CURRENT_TIMESTAMP, pd.status = 'DELETED' WHERE pd.productDetailId = :id")
    int softDeleteById(@Param("id") Integer id);

    @Modifying
    @Transactional
    @Query("UPDATE ProductDetail pd SET pd.deletedAt = CURRENT_TIMESTAMP, pd.status = 'DELETED' WHERE pd.product.productId = :productId AND pd.deletedAt IS NULL")
    void softDeleteByProductId(@Param("productId") Integer productId);

    // Thêm vào ProductDetailRepository.java
    @Query("SELECT pd FROM ProductDetail pd WHERE pd.product.productId IN :productIds AND pd.deletedAt IS NULL")
    List<ProductDetail> findAllByProductIds(@Param("productIds") List<Integer> productIds);


    // ========== METHOD CHO POS (BÁN HÀNG TẠI QUẦY) ==========

    // Trừ tồn kho khi bán hàng
    @Modifying
    @Transactional
    @Query("UPDATE ProductDetail pd SET pd.quantity = pd.quantity - :quantity, pd.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE pd.productDetailId = :productDetailId AND pd.quantity >= :quantity")
    int decreaseQuantity(@Param("productDetailId") Integer productDetailId,
                         @Param("quantity") Integer quantity);

    // Cộng tồn kho khi hoàn hàng
    @Modifying
    @Transactional
    @Query("UPDATE ProductDetail pd SET pd.quantity = pd.quantity + :quantity, pd.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE pd.productDetailId = :productDetailId")
    void increaseQuantity(@Param("productDetailId") Integer productDetailId,
                          @Param("quantity") Integer quantity);

    // Kiểm tra tồn kho
    @Query("SELECT pd.quantity FROM ProductDetail pd WHERE pd.productDetailId = :productDetailId")
    Integer getQuantityById(@Param("productDetailId") Integer productDetailId);

    // Lấy thông tin sản phẩm chi tiết kèm Product, Color, Size
    @Query("SELECT pd FROM ProductDetail pd " +
            "LEFT JOIN FETCH pd.product " +
            "LEFT JOIN FETCH pd.color " +
            "LEFT JOIN FETCH pd.size " +
            "WHERE pd.productDetailId = :id AND pd.deletedAt IS NULL")
    Optional<ProductDetail> findByIdWithRelations(@Param("id") Integer id);
}