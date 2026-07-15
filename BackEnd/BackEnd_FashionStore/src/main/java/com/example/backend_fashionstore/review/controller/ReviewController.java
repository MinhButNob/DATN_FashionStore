package com.example.backend_fashionstore.review.controller;

import com.example.backend_fashionstore.review.dto.request.ReviewRequestDTO;
import com.example.backend_fashionstore.review.dto.response.ReviewResponseDTO;
import com.example.backend_fashionstore.review.dto.response.ProductRatingDTO;
import com.example.backend_fashionstore.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    private static final String UPLOAD_DIR = "uploads/reviews/";
    private static final String BASE_URL = "http://localhost:8080";

    // ===== REVIEW CRUD =====

    @PostMapping("/create")
    public ResponseEntity<ReviewResponseDTO> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewRequestDTO request
    ) {
        try {
            Integer customerId = getCustomerIdFromUserDetails(userDetails);
            ReviewResponseDTO review = reviewService.createReview(customerId, request);
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            log.error("❌ Error creating review: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/product/{productDetailId}")
    public ResponseEntity<Page<ReviewResponseDTO>> getProductReviews(
            @PathVariable Integer productDetailId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewResponseDTO> reviews = reviewService.getProductReviews(productDetailId, pageable);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/product/{productDetailId}/rating")
    public ResponseEntity<ProductRatingDTO> getProductRating(@PathVariable Integer productDetailId) {
        ProductRatingDTO rating = reviewService.getProductRating(productDetailId);
        return ResponseEntity.ok(rating);
    }

    // ✅ Lấy review theo productId (gộp tất cả biến thể màu/size của 1 sản phẩm)
    @GetMapping("/product-all/{productId}")
    public ResponseEntity<Page<ReviewResponseDTO>> getAllReviewsByProduct(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewResponseDTO> reviews = reviewService.getProductReviewsByProductId(productId, pageable);
        return ResponseEntity.ok(reviews);
    }

    // ✅ Lấy thống kê rating theo productId (gộp tất cả biến thể màu/size)
    @GetMapping("/product-all/{productId}/rating")
    public ResponseEntity<ProductRatingDTO> getProductRatingByProduct(@PathVariable Integer productId) {
        ProductRatingDTO rating = reviewService.getProductRatingByProductId(productId);
        return ResponseEntity.ok(rating);
    }

    @GetMapping("/my-reviews")
    public ResponseEntity<Page<ReviewResponseDTO>> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewResponseDTO> reviews = reviewService.getMyReviews(customerId, pageable);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkReviewed(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long orderId,
            @RequestParam Integer productDetailId
    ) {
        try {
            Integer customerId = getCustomerIdFromUserDetails(userDetails);
            boolean reviewed = reviewService.checkReviewed(customerId, orderId, productDetailId);
            Map<String, Boolean> response = new HashMap<>();
            response.put("reviewed", reviewed);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error checking review: {}", e.getMessage(), e);
            Map<String, Boolean> response = new HashMap<>();
            response.put("reviewed", false);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/can-review")
    public ResponseEntity<Map<String, Boolean>> canReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long orderId,
            @RequestParam Integer productDetailId
    ) {
        try {
            Integer customerId = getCustomerIdFromUserDetails(userDetails);
            boolean canReview = reviewService.canReview(customerId, orderId, productDetailId);

            log.info("✅ canReview result - customerId: {}, orderId: {}, productDetailId: {}, result: {}",
                    customerId, orderId, productDetailId, canReview);

            Map<String, Boolean> response = new HashMap<>();
            response.put("canReview", canReview);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error checking canReview: {}", e.getMessage(), e);
            Map<String, Boolean> response = new HashMap<>();
            response.put("canReview", false);
            return ResponseEntity.ok(response);
        }
    }

    // ✅ THÊM API XÓA ĐÁNH GIÁ
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Integer reviewId
    ) {
        try {
            Integer customerId = getCustomerIdFromUserDetails(userDetails);
            reviewService.deleteReview(reviewId, customerId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Xóa đánh giá thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error deleting review: {}", e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ✅ THÊM API SỬA ĐÁNH GIÁ
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> updateReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Integer reviewId,
            @Valid @RequestBody ReviewRequestDTO request
    ) {
        try {
            Integer customerId = getCustomerIdFromUserDetails(userDetails);
            ReviewResponseDTO review = reviewService.updateReview(reviewId, customerId, request);
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            log.error("❌ Error updating review: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ✅ THÊM API LẤY CHI TIẾT ĐÁNH GIÁ
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> getReviewDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Integer reviewId
    ) {
        try {
            Integer customerId = getCustomerIdFromUserDetails(userDetails);
            ReviewResponseDTO review = reviewService.getReviewDetail(reviewId, customerId);
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            log.error("❌ Error getting review detail: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ===== UPLOAD IMAGE =====

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadReviewImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            log.info("📤 Uploading review image: {}", file.getOriginalFilename());

            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File rỗng"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ hỗ trợ file ảnh"));
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "File quá lớn (tối đa 5MB)"));
            }

            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
                log.info("📁 Created directory: {}", uploadDir.getAbsolutePath());
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String filename = "review_" + timestamp + "_" + uniqueId + extension;

            Path path = Paths.get(UPLOAD_DIR + filename);
            Files.write(path, file.getBytes());

            String imageUrl = BASE_URL + "/uploads/reviews/" + filename;

            Map<String, String> response = new HashMap<>();
            response.put("url", imageUrl);
            response.put("filename", filename);

            log.info("✅ Image uploaded: {}", imageUrl);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("❌ Upload error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi khi tải ảnh lên: " + e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Upload error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi không xác định: " + e.getMessage()));
        }
    }

    // ===== HELPER METHODS =====

    private Integer getCustomerIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Vui lòng đăng nhập");
        }
        String email = userDetails.getUsername();
        return reviewService.getCustomerIdByEmail(email);
    }
}