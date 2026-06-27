package com.example.backend_fashionstore.review.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReviewResponseDTO {
    private Integer reviewId;
    private Long orderId;
    private Integer productDetailId;
    private String productName;
    private String colorName;
    private String sizeName;
    private String productImageUrl;
    private Integer rating;
    private String comment;
    private List<String> images;  // ✅ THÊM
    private String reviewStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String customerName;
    private String customerEmail;
}