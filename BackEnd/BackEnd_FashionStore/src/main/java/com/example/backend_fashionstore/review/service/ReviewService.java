package com.example.backend_fashionstore.review.service;

import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderDetailRepository;
import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderRepository;
import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import com.example.backend_fashionstore.quanLyKhachHang.repository.KhachHangCustomerRepository;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.review.dto.request.ReviewRequestDTO;
import com.example.backend_fashionstore.review.dto.response.ProductRatingDTO;
import com.example.backend_fashionstore.review.dto.response.ReviewResponseDTO;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductDetailRepository productDetailRepository;
    private final KhachHangCustomerRepository customerRepository;

    @Transactional
    public ReviewResponseDTO createReview(Integer customerId, ReviewRequestDTO request) {
        log.info("🔄 Creating review for customer: {}, product: {}, order: {}",
                customerId, request.getProductDetailId(), request.getOrderId());

        // 1. Kiểm tra sản phẩm tồn tại
        ProductDetail productDetail = productDetailRepository.findById(request.getProductDetailId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // 2. Kiểm tra khách hàng
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        // 3. Kiểm tra đã đánh giá sản phẩm này trong đơn hàng này chưa
        boolean alreadyReviewed = reviewRepository.existsByCustomerIdAndProductDetailIdAndOrderId(
                customerId, request.getProductDetailId(), request.getOrderId()
        );

        if (alreadyReviewed) {
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này trong đơn hàng này rồi!");
        }

        // 4. Kiểm tra đã hoàn thành đơn hàng chưa (COMPLETED)
        boolean hasCompleted = orderRepository.hasCustomerCompletedOrder(customerId, request.getProductDetailId());
        log.info("✅ Has completed order: {}", hasCompleted);

        if (!hasCompleted) {
            throw new RuntimeException("Bạn chỉ có thể đánh giá sản phẩm sau khi đơn hàng đã hoàn thành!");
        }

        // 5. Chuyển đổi images sang JSON string
        String imagesJson = null;
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                imagesJson = objectMapper.writeValueAsString(request.getImages());
                log.info("✅ Images converted to JSON: {}", imagesJson);
            } catch (Exception e) {
                log.warn("Cannot convert images to JSON: {}", e.getMessage());
            }
        }

        // 6. Tạo review
        Review review = Review.builder()
                .productDetailId(request.getProductDetailId())
                .customerId(customerId)
                .orderId(request.getOrderId())
                .rating(request.getRating())
                .comment(request.getComment())
                .images(imagesJson)
                .reviewStatus("APPROVED")
                .build();

        review = reviewRepository.save(review);
        log.info("✅ Review created with ID: {}", review.getReviewId());

        return convertToResponse(review, productDetail, customer);
    }

    public Page<ReviewResponseDTO> getProductReviews(Integer productDetailId, Pageable pageable) {
        return reviewRepository.findApprovedByProductDetailId(productDetailId, pageable)
                .map(review -> {
                    ProductDetail pd = productDetailRepository.findById(productDetailId).orElse(null);
                    Customer customer = customerRepository.findById(review.getCustomerId()).orElse(null);
                    return convertToResponse(review, pd, customer);
                });
    }

    public ProductRatingDTO getProductRating(Integer productDetailId) {
        Double avgRating = reviewRepository.getAverageRatingByProductDetailId(productDetailId);
        Integer totalReviews = reviewRepository.getTotalReviewsByProductDetailId(productDetailId);

        List<Object[]> ratingCounts = reviewRepository.getRatingCountsByProductDetailId(productDetailId);
        int rating1 = 0, rating2 = 0, rating3 = 0, rating4 = 0, rating5 = 0;

        for (Object[] count : ratingCounts) {
            Integer rating = (Integer) count[0];
            Long total = (Long) count[1];
            if (rating == 1) rating1 = total.intValue();
            else if (rating == 2) rating2 = total.intValue();
            else if (rating == 3) rating3 = total.intValue();
            else if (rating == 4) rating4 = total.intValue();
            else if (rating == 5) rating5 = total.intValue();
        }

        return ProductRatingDTO.builder()
                .productDetailId(productDetailId)
                .averageRating(avgRating != null ? avgRating : 0.0)
                .totalReviews(totalReviews != null ? totalReviews : 0)
                .rating1Count(rating1)
                .rating2Count(rating2)
                .rating3Count(rating3)
                .rating4Count(rating4)
                .rating5Count(rating5)
                .build();
    }

    public Page<ReviewResponseDTO> getMyReviews(Integer customerId, Pageable pageable) {
        return reviewRepository.findByCustomerId(customerId, pageable)
                .map(review -> {
                    ProductDetail pd = productDetailRepository.findById(review.getProductDetailId()).orElse(null);
                    Customer customer = customerRepository.findById(customerId).orElse(null);
                    return convertToResponse(review, pd, customer);
                });
    }

    public boolean checkReviewed(Integer customerId, Long orderId, Integer productDetailId) {
        try {
            return reviewRepository.existsByOrderAndProduct(orderId, productDetailId, customerId);
        } catch (Exception e) {
            log.error("❌ Error checking reviewed: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean checkReviewedByCustomerAndProduct(Integer customerId, Integer productDetailId) {
        try {
            return reviewRepository.existsByCustomerIdAndProductDetailIdAndDeletedAtIsNull(customerId, productDetailId);
        } catch (Exception e) {
            log.error("❌ Error checking reviewed by customer: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean canReview(Integer customerId, Long orderId, Integer productDetailId) {
        try {
            log.info("🔍 canReview - customerId: {}, orderId: {}, productDetailId: {}",
                    customerId, orderId, productDetailId);

            // 1. Kiểm tra đã đánh giá sản phẩm này trong đơn hàng này chưa
            boolean alreadyReviewed = reviewRepository.existsByCustomerIdAndProductDetailIdAndOrderId(
                    customerId, productDetailId, orderId
            );

            if (alreadyReviewed) {
                log.info("❌ Already reviewed in this order");
                return false;
            }

            // 2. Kiểm tra đơn hàng đã COMPLETED chưa
            boolean hasCompleted = orderRepository.hasCustomerCompletedOrder(customerId, productDetailId);
            log.info("✅ Has completed order: {}", hasCompleted);

            return hasCompleted;
        } catch (Exception e) {
            log.error("❌ Error in canReview: {}", e.getMessage(), e);
            return false;
        }
    }

    public Integer getCustomerIdByEmail(String email) {
        return customerRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với email: " + email))
                .getCustomerId();
    }

    // ✅ XÓA ĐÁNH GIÁ - Cho phép khách hàng xóa review của chính mình (không phân biệt trạng thái)
    @Transactional
    public void deleteReview(Integer reviewId, Integer customerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        // Kiểm tra quyền sở hữu - Chỉ cho phép xóa review của chính mình
        if (!review.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Bạn không có quyền xóa đánh giá này");
        }

        // ❌ ĐÃ XÓA KIỂM TRA TRẠNG THÁI - Cho phép xóa mọi trạng thái
        // if ("APPROVED".equals(review.getReviewStatus())) {
        //     throw new RuntimeException("Không thể xóa đánh giá đã được duyệt. Vui lòng liên hệ admin.");
        // }

        // Xóa mềm (soft delete)
        review.setDeletedAt(LocalDateTime.now());
        reviewRepository.save(review);

        log.info("🗑️ Review {} deleted by customer {}", reviewId, customerId);
    }

    // ✅ SỬA ĐÁNH GIÁ - Cho phép khách hàng sửa review của chính mình (không phân biệt trạng thái)
    @Transactional
    public ReviewResponseDTO updateReview(Integer reviewId, Integer customerId, ReviewRequestDTO request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        // Kiểm tra quyền sở hữu
        if (!review.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Bạn không có quyền sửa đánh giá này");
        }

        // ❌ ĐÃ XÓA KIỂM TRA TRẠNG THÁI - Cho phép sửa mọi trạng thái
        // if ("APPROVED".equals(review.getReviewStatus())) {
        //     throw new RuntimeException("Không thể sửa đánh giá đã được duyệt. Vui lòng liên hệ admin.");
        // }

        // Cập nhật thông tin
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        // Cập nhật images
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String imagesJson = objectMapper.writeValueAsString(request.getImages());
                review.setImages(imagesJson);
            } catch (Exception e) {
                log.warn("Cannot convert images to JSON: {}", e.getMessage());
            }
        }

        review.setUpdatedAt(LocalDateTime.now());
        review = reviewRepository.save(review);

        log.info("✏️ Review {} updated by customer {}", reviewId, customerId);

        // Lấy thông tin sản phẩm và khách hàng để trả về
        ProductDetail productDetail = productDetailRepository.findById(review.getProductDetailId()).orElse(null);
        Customer customer = customerRepository.findById(customerId).orElse(null);

        return convertToResponse(review, productDetail, customer);
    }

    // ✅ LẤY CHI TIẾT ĐÁNH GIÁ
    public ReviewResponseDTO getReviewDetail(Integer reviewId, Integer customerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        // Kiểm tra quyền sở hữu
        if (!review.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Bạn không có quyền xem đánh giá này");
        }

        ProductDetail productDetail = productDetailRepository.findById(review.getProductDetailId()).orElse(null);
        Customer customer = customerRepository.findById(customerId).orElse(null);

        return convertToResponse(review, productDetail, customer);
    }

    private ReviewResponseDTO convertToResponse(Review review, ProductDetail productDetail, Customer customer) {
        String productName = "";
        String colorName = "";
        String sizeName = "";
        String imageUrl = "";

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
                imageUrl = productDetail.getProductImages().get(0).getProductImagePath();
            }
        }

        // Parse images từ JSON string sang List
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

        return ReviewResponseDTO.builder()
                .reviewId(review.getReviewId())
                .productDetailId(review.getProductDetailId())
                .productName(productName)
                .colorName(colorName)
                .sizeName(sizeName)
                .imageUrl(imageUrl)
                .images(images)
                .rating(review.getRating())
                .comment(review.getComment())
                .reviewStatus(review.getReviewStatus())
                .createdAt(review.getCreatedAt())
                .customerName(customer != null ? customer.getCustomerName() : null)
                .customerEmail(customer != null && customer.getUser() != null ? customer.getUser().getEmail() : null)
                .orderId(review.getOrderId())
                .build();
    }
}