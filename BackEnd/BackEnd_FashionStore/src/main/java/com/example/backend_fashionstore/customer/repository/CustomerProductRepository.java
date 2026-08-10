package com.example.backend_fashionstore.customer.repository;

import com.example.backend_fashionstore.entity.Color;
import com.example.backend_fashionstore.entity.Product;
import com.example.backend_fashionstore.entity.ProductDetail;
import com.example.backend_fashionstore.entity.ProductImage;
import com.example.backend_fashionstore.entity.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerProductRepository extends JpaRepository<Product, Integer> {

    // Lấy sản phẩm đang ACTIVE và chưa bị xóa
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND p.deletedAt IS NULL")
    Page<Product> findAllActiveProducts(Pageable pageable);

    // Tìm kiếm sản phẩm với filter
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN p.productDetails pd " +
            "WHERE p.status = 'ACTIVE' AND p.deletedAt IS NULL " +
            "AND (:keyword IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId) " +
            "AND (:colorId IS NULL OR pd.color.colorId = :colorId) " +
            "AND (:sizeId IS NULL OR pd.size.sizeId = :sizeId) " +
            "AND (:minPrice IS NULL OR pd.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR pd.price <= :maxPrice)")
    Page<Product> filterProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            @Param("colorId") Integer colorId,
            @Param("sizeId") Integer sizeId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    // Lấy chi tiết sản phẩm kèm variants
    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.productDetails pd " +
            "LEFT JOIN FETCH pd.color " +
            "LEFT JOIN FETCH pd.size " +
            "WHERE p.productId = :id AND p.status = 'ACTIVE' AND p.deletedAt IS NULL")
    Optional<Product> findProductDetailById(@Param("id") Integer id);

    // Lấy tất cả biến thể của sản phẩm (còn hàng và đang hoạt động)
    @Query("SELECT pd FROM ProductDetail pd " +
            "WHERE pd.product.productId = :productId " +
            "AND pd.status = 'ACTIVE' AND pd.deletedAt IS NULL")
    List<ProductDetail> findActiveVariantsByProductId(@Param("productId") Integer productId);

    // Lấy ảnh của biến thể
    @Query("SELECT pi FROM ProductImage pi " +
            "WHERE pi.productDetail.productDetailId = :productDetailId " +
            "AND pi.status = 'ACTIVE' AND pi.deletedAt IS NULL " +
            "ORDER BY pi.isMain DESC, pi.displayOrder ASC")
    List<ProductImage> findActiveImagesByVariantId(@Param("productDetailId") Integer productDetailId);

    // Lấy ảnh chính của sản phẩm
    @Query("SELECT pi FROM ProductImage pi " +
            "WHERE pi.productDetail.product.productId = :productId " +
            "AND pi.status = 'ACTIVE' AND pi.deletedAt IS NULL " +
            "ORDER BY pi.isMain DESC, pi.displayOrder ASC")
    List<ProductImage> findMainImagesByProductId(@Param("productId") Integer productId);

    // Lấy danh sách màu sắc có sẵn của sản phẩm
    @Query("SELECT DISTINCT c FROM Color c " +
            "JOIN ProductDetail pd ON pd.color.colorId = c.colorId " +
            "WHERE pd.product.productId = :productId " +
            "AND pd.status = 'ACTIVE' AND pd.deletedAt IS NULL " +
            "AND c.status = 'ACTIVE' AND c.deletedAt IS NULL")
    List<Color> getAvailableColors(@Param("productId") Integer productId);

    // Lấy danh sách kích thước có sẵn của sản phẩm
    @Query("SELECT DISTINCT s FROM Size s " +
            "JOIN ProductDetail pd ON pd.size.sizeId = s.sizeId " +
            "WHERE pd.product.productId = :productId " +
            "AND pd.status = 'ACTIVE' AND pd.deletedAt IS NULL " +
            "AND s.status = 'ACTIVE' AND s.deletedAt IS NULL")
    List<Size> getAvailableSizes(@Param("productId") Integer productId);

    // Lấy khoảng giá của sản phẩm
    @Query("SELECT MIN(pd.price), MAX(pd.price) FROM ProductDetail pd " +
            "WHERE pd.product.productId = :productId " +
            "AND pd.status = 'ACTIVE' AND pd.deletedAt IS NULL")
    Object[] getPriceRange(@Param("productId") Integer productId);

    // Đếm tổng số sản phẩm đang hoạt động
    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = 'ACTIVE' AND p.deletedAt IS NULL")
    long countActiveProducts();

    // Lấy sản phẩm mới nhất
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    Page<Product> findLatestProducts(Pageable pageable);

    // Lấy sản phẩm theo danh mục
    @Query("SELECT p FROM Product p WHERE p.category.categoryId = :categoryId AND p.status = 'ACTIVE' AND p.deletedAt IS NULL")
    Page<Product> findProductsByCategory(@Param("categoryId") Integer categoryId, Pageable pageable);

    // Lấy sản phẩm nổi bật (fallback khi chưa đủ sản phẩm bán chạy thật)
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND p.deletedAt IS NULL ORDER BY p.productId DESC")
    Page<Product> findFeaturedProducts(Pageable pageable);

    // ✅ Lấy sản phẩm BÁN CHẠY THẬT dựa trên số lượng đã bán từ order_details
    // (chỉ tính các đơn hàng hợp lệ, loại trừ đơn đã hủy/bị từ chối/hoàn trả)
    @Query(value =
            "SELECT p.* FROM products p " +
                    "JOIN product_details pd ON pd.product_detail_product_id = p.product_id " +
                    "JOIN order_details od ON od.order_detail_product_detail_id = pd.product_detail_id " +
                    "JOIN orders o ON o.order_id = od.order_detail_order_id " +
                    "WHERE p.product_status = 'ACTIVE' AND p.product_deleted_at IS NULL " +
                    "AND o.order_deleted_at IS NULL " +
                    "AND o.order_status NOT IN ('CANCELLED','REJECTED','RETURNED','RETURN_REQUESTED','RETURN_APPROVED','RETURN_RECEIVED','RETURN_PENDING') " +
                    "GROUP BY p.product_id " +
                    "ORDER BY SUM(od.order_detail_quantity) DESC",
            countQuery =
                    "SELECT COUNT(DISTINCT p.product_id) FROM products p " +
                            "JOIN product_details pd ON pd.product_detail_product_id = p.product_id " +
                            "JOIN order_details od ON od.order_detail_product_detail_id = pd.product_detail_id " +
                            "JOIN orders o ON o.order_id = od.order_detail_order_id " +
                            "WHERE p.product_status = 'ACTIVE' AND p.product_deleted_at IS NULL " +
                            "AND o.order_deleted_at IS NULL " +
                            "AND o.order_status NOT IN ('CANCELLED','REJECTED','RETURNED','RETURN_REQUESTED','RETURN_APPROVED','RETURN_RECEIVED','RETURN_PENDING')",
            nativeQuery = true)
    Page<Product> findRealBestSellingProducts(Pageable pageable);

    // Lấy danh sách productId đã có trong best-seller thật (dùng để loại trừ khi fallback)
    @Query(value =
            "SELECT DISTINCT p.product_id FROM products p " +
                    "JOIN product_details pd ON pd.product_detail_product_id = p.product_id " +
                    "JOIN order_details od ON od.order_detail_product_detail_id = pd.product_detail_id " +
                    "JOIN orders o ON o.order_id = od.order_detail_order_id " +
                    "WHERE p.product_status = 'ACTIVE' AND p.product_deleted_at IS NULL " +
                    "AND o.order_deleted_at IS NULL " +
                    "AND o.order_status NOT IN ('CANCELLED','REJECTED','RETURNED','RETURN_REQUESTED','RETURN_APPROVED','RETURN_RECEIVED','RETURN_PENDING')",
            nativeQuery = true)
    List<Integer> findProductIdsWithRealSales();

    // ✅ Số lượng đã bán THẬT của 1 sản phẩm (chỉ tính đơn hàng hợp lệ)
    @Query(value =
            "SELECT COALESCE(SUM(od.order_detail_quantity), 0) " +
                    "FROM product_details pd " +
                    "JOIN order_details od ON od.order_detail_product_detail_id = pd.product_detail_id " +
                    "JOIN orders o ON o.order_id = od.order_detail_order_id " +
                    "WHERE pd.product_detail_product_id = :productId " +
                    "AND o.order_deleted_at IS NULL " +
                    "AND o.order_status NOT IN ('CANCELLED','REJECTED','RETURNED','RETURN_REQUESTED','RETURN_APPROVED','RETURN_RECEIVED','RETURN_PENDING')",
            nativeQuery = true)
    Integer getSoldCountByProductId(@Param("productId") Integer productId);

    // ✅ Lấy sản phẩm ĐÃ CÓ NGƯỜI MUA VÀ ĐÁNH GIÁ THẬT (review approved),
    // sắp xếp theo ĐIỂM ĐÁNH GIÁ TRUNG BÌNH cao nhất trước (đúng ngữ nghĩa "được đánh giá cao"),
    // nếu bằng điểm thì ưu tiên sản phẩm có nhiều lượt đánh giá hơn.
    @Query(value =
            "SELECT p.* FROM products p " +
                    "JOIN product_details pd ON pd.product_detail_product_id = p.product_id " +
                    "JOIN reviews r ON r.product_detail_id = pd.product_detail_id " +
                    "WHERE p.product_status = 'ACTIVE' AND p.product_deleted_at IS NULL " +
                    "AND r.review_status = 'APPROVED' AND r.deleted_at IS NULL " +
                    "GROUP BY p.product_id " +
                    "ORDER BY AVG(r.rating) DESC, COUNT(r.review_id) DESC, p.product_id DESC",
            countQuery =
                    "SELECT COUNT(DISTINCT p.product_id) FROM products p " +
                            "JOIN product_details pd ON pd.product_detail_product_id = p.product_id " +
                            "JOIN reviews r ON r.product_detail_id = pd.product_detail_id " +
                            "WHERE p.product_status = 'ACTIVE' AND p.product_deleted_at IS NULL " +
                            "AND r.review_status = 'APPROVED' AND r.deleted_at IS NULL",
            nativeQuery = true)
    Page<Product> findTopRatedProducts(Pageable pageable);
}