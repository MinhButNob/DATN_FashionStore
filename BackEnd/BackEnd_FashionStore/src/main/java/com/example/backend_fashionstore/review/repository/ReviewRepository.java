package com.example.backend_fashionstore.review.repository;

import com.example.backend_fashionstore.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // Lấy đánh giá theo sản phẩm (approved)
    @Query("SELECT r FROM Review r WHERE r.productDetailId = :productDetailId AND r.reviewStatus = 'APPROVED' AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Page<Review> findApprovedByProductDetailId(@Param("productDetailId") Integer productDetailId, Pageable pageable);

    // ✅ Kiểm tra đã đánh giá trong đơn hàng này chưa
    @Query("SELECT COUNT(r) > 0 FROM Review r WHERE r.customerId = :customerId AND r.productDetailId = :productDetailId AND r.orderId = :orderId AND r.deletedAt IS NULL")
    boolean existsByCustomerIdAndProductDetailIdAndOrderId(
            @Param("customerId") Integer customerId,
            @Param("productDetailId") Integer productDetailId,
            @Param("orderId") Long orderId
    );

    // ✅ Kiểm tra đã đánh giá bất kỳ đơn hàng nào (KHÔNG DÙNG NỮA, nhưng giữ để tương thích)
    @Query("SELECT COUNT(r) > 0 FROM Review r WHERE r.customerId = :customerId AND r.productDetailId = :productDetailId AND r.deletedAt IS NULL")
    boolean existsByCustomerIdAndProductDetailIdAndDeletedAtIsNull(
            @Param("customerId") Integer customerId,
            @Param("productDetailId") Integer productDetailId
    );

    // ✅ Kiểm tra đánh giá theo order
    @Query("SELECT COUNT(r) > 0 FROM Review r WHERE r.orderId = :orderId AND r.productDetailId = :productDetailId AND r.customerId = :customerId AND r.deletedAt IS NULL")
    boolean existsByOrderAndProduct(
            @Param("orderId") Long orderId,
            @Param("productDetailId") Integer productDetailId,
            @Param("customerId") Integer customerId
    );

    // Lấy đánh giá của khách hàng theo đơn hàng
    @Query("SELECT r FROM Review r WHERE r.orderId = :orderId AND r.customerId = :customerId AND r.deletedAt IS NULL")
    List<Review> findByOrderIdAndCustomerId(@Param("orderId") Long orderId, @Param("customerId") Integer customerId);

    // Lấy tất cả đánh giá của khách hàng
    @Query("SELECT r FROM Review r WHERE r.customerId = :customerId AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Page<Review> findByCustomerId(@Param("customerId") Integer customerId, Pageable pageable);

    // ✅ Lấy review theo customer và product (không phân biệt order)
    @Query("SELECT r FROM Review r WHERE r.customerId = :customerId AND r.productDetailId = :productDetailId AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    List<Review> findByCustomerIdAndProductDetailIdAndDeletedAtIsNull(
            @Param("customerId") Integer customerId,
            @Param("productDetailId") Integer productDetailId
    );

    // ===== LẤY REVIEW THEO PRODUCT (TẤT CẢ BIẾN THỂ MÀU/SIZE) =====

    @Query("SELECT r FROM Review r JOIN ProductDetail pd ON r.productDetailId = pd.productDetailId " +
            "WHERE pd.product.productId = :productId AND r.reviewStatus = 'APPROVED' AND r.deletedAt IS NULL " +
            "ORDER BY r.createdAt DESC")
    Page<Review> findApprovedByProductId(@Param("productId") Integer productId, Pageable pageable);

    @Query("SELECT r FROM Review r JOIN ProductDetail pd ON r.productDetailId = pd.productDetailId " +
            "WHERE pd.product.productId = :productId AND r.rating = :rating AND r.reviewStatus = 'APPROVED' AND r.deletedAt IS NULL " +
            "ORDER BY r.createdAt DESC")
    Page<Review> findApprovedByProductIdAndRating(@Param("productId") Integer productId, @Param("rating") Integer rating, Pageable pageable);


    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r JOIN ProductDetail pd ON r.productDetailId = pd.productDetailId " +
            "WHERE pd.product.productId = :productId AND r.reviewStatus = 'APPROVED' AND r.deletedAt IS NULL")
    Double getAverageRatingByProductId(@Param("productId") Integer productId);

    @Query("SELECT COUNT(r) FROM Review r JOIN ProductDetail pd ON r.productDetailId = pd.productDetailId " +
            "WHERE pd.product.productId = :productId AND r.reviewStatus = 'APPROVED' AND r.deletedAt IS NULL")
    Integer getTotalReviewsByProductId(@Param("productId") Integer productId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r JOIN ProductDetail pd ON r.productDetailId = pd.productDetailId " +
            "WHERE pd.product.productId = :productId AND r.reviewStatus = 'APPROVED' AND r.deletedAt IS NULL GROUP BY r.rating")
    List<Object[]> getRatingCountsByProductId(@Param("productId") Integer productId);

    // ===== ADMIN METHODS =====

    @Query("SELECT r FROM Review r WHERE r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Page<Review> findAllByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.reviewStatus = :status AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Page<Review> findByReviewStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT r FROM Review r LEFT JOIN ProductDetail pd ON r.productDetailId = pd.productDetailId " +
            "LEFT JOIN Product p ON pd.product.productId = p.productId " +
            "LEFT JOIN Customer c ON r.customerId = c.customerId " +
            "WHERE r.deletedAt IS NULL AND " +
            "(LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(r.comment) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY r.createdAt DESC")
    Page<Review> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT r FROM Review r LEFT JOIN ProductDetail pd ON r.productDetailId = pd.productDetailId " +
            "LEFT JOIN Product p ON pd.product.productId = p.productId " +
            "LEFT JOIN Customer c ON r.customerId = c.customerId " +
            "WHERE r.deletedAt IS NULL AND r.reviewStatus = :status AND " +
            "(LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(r.comment) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY r.createdAt DESC")
    Page<Review> findByStatusAndKeyword(
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewStatus = :status AND r.deletedAt IS NULL")
    long countByReviewStatusAndDeletedAtIsNull(@Param("status") String status);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.deletedAt IS NULL")
    long countByDeletedAtIsNull();

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.reviewStatus = 'APPROVED' AND r.deletedAt IS NULL")
    Double getAverageRatingAll();

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.productDetailId = :productDetailId AND r.reviewStatus = 'APPROVED' AND r.deletedAt IS NULL")
    Double getAverageRatingByProductDetailId(@Param("productDetailId") Integer productDetailId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.productDetailId = :productDetailId AND r.reviewStatus = 'APPROVED' AND r.deletedAt IS NULL")
    Integer getTotalReviewsByProductDetailId(@Param("productDetailId") Integer productDetailId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.productDetailId = :productDetailId AND r.reviewStatus = 'APPROVED' AND r.deletedAt IS NULL GROUP BY r.rating")
    List<Object[]> getRatingCountsByProductDetailId(@Param("productDetailId") Integer productDetailId);
}