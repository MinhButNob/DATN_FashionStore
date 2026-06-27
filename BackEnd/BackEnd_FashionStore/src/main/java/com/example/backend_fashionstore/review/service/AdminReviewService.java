package com.example.backend_fashionstore.review.service;

import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import com.example.backend_fashionstore.entity.Customer;
import com.example.backend_fashionstore.entity.Product;
import com.example.backend_fashionstore.entity.ProductDetail;
import com.example.backend_fashionstore.entity.Review;
import com.example.backend_fashionstore.quanLyKhachHang.repository.KhachHangCustomerRepository;
import com.example.backend_fashionstore.review.dto.response.AdminReviewResponseDTO;
import com.example.backend_fashionstore.review.repository.ReviewRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductDetailRepository productDetailRepository;
    private final KhachHangCustomerRepository customerRepository;
    // ❌ ĐÃ XÓA: private final ObjectMapper objectMapper;

    public Page<AdminReviewResponseDTO> getAllReviews(String status, String keyword, Pageable pageable) {
        Page<Review> reviews;

        if (status != null && !status.isEmpty() && keyword != null && !keyword.isEmpty()) {
            reviews = reviewRepository.findByStatusAndKeyword(status, keyword, pageable);
        } else if (status != null && !status.isEmpty()) {
            reviews = reviewRepository.findByReviewStatus(status, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            reviews = reviewRepository.findByKeyword(keyword, pageable);
        } else {
            reviews = reviewRepository.findAllByDeletedAtIsNull(pageable);
        }

        return reviews.map(this::convertToAdminResponse);
    }

    public AdminReviewResponseDTO getReviewDetail(Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));
        return convertToAdminResponse(review);
    }

    public AdminReviewResponseDTO approveReview(Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        review.setReviewStatus("APPROVED");
        review = reviewRepository.save(review);

        log.info("✅ Review {} approved", reviewId);
        return convertToAdminResponse(review);
    }

    public AdminReviewResponseDTO rejectReview(Integer reviewId, String reason) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        review.setReviewStatus("REJECTED");
        review = reviewRepository.save(review);

        log.info("❌ Review {} rejected. Reason: {}", reviewId, reason);
        return convertToAdminResponse(review);
    }

    public void deleteReview(Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        review.setDeletedAt(LocalDateTime.now());
        reviewRepository.save(review);

        log.info("🗑️ Review {} deleted", reviewId);
    }

    public Map<String, Object> getReviewStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalReviews = reviewRepository.countByDeletedAtIsNull();
        long pendingReviews = reviewRepository.countByReviewStatusAndDeletedAtIsNull("PENDING");
        long approvedReviews = reviewRepository.countByReviewStatusAndDeletedAtIsNull("APPROVED");
        long rejectedReviews = reviewRepository.countByReviewStatusAndDeletedAtIsNull("REJECTED");

        Double averageRating = reviewRepository.getAverageRatingAll();

        stats.put("totalReviews", totalReviews);
        stats.put("pendingReviews", pendingReviews);
        stats.put("approvedReviews", approvedReviews);
        stats.put("rejectedReviews", rejectedReviews);
        stats.put("averageRating", averageRating != null ? averageRating : 0.0);

        return stats;
    }

    private AdminReviewResponseDTO convertToAdminResponse(Review review) {
        ProductDetail productDetail = productDetailRepository.findById(review.getProductDetailId()).orElse(null);
        Customer customer = customerRepository.findById(review.getCustomerId()).orElse(null);

        String productName = "";
        String colorName = "";
        String sizeName = "";
        String productImageUrl = "";

        if (productDetail != null) {
            Product product = productDetail.getProduct();
            if (product != null) {
                productName = product.getProductName();
            }
            if (productDetail.getColor() != null) {
                colorName = productDetail.getColor().getColorName();
            }
            if (productDetail.getSize() != null) {
                sizeName = productDetail.getSize().getSizeName();
            }
            if (productDetail.getProductImages() != null && !productDetail.getProductImages().isEmpty()) {
                productImageUrl = productDetail.getProductImages().get(0).getProductImagePath();
            }
        }

        // Parse images từ JSON string sang List - ✅ Tạo ObjectMapper mới trực tiếp
        List<String> images = null;
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                images = objectMapper.readValue(
                        review.getImages(),
                        new TypeReference<List<String>>() {}
                );
            } catch (Exception e) {
                log.warn("Cannot parse images: {}", e.getMessage());
            }
        }

        return AdminReviewResponseDTO.builder()
                .reviewId(review.getReviewId())
                .orderId(review.getOrderId())
                .productDetailId(review.getProductDetailId())
                .productName(productName)
                .colorName(colorName)
                .sizeName(sizeName)
                .productImageUrl(productImageUrl)
                .rating(review.getRating())
                .comment(review.getComment())
                .images(images)
                .reviewStatus(review.getReviewStatus())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .customerName(customer != null ? customer.getCustomerName() : null)
                .customerEmail(customer != null && customer.getUser() != null ? customer.getUser().getEmail() : null)
                .build();
    }
}