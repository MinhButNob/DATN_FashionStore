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
public class ReviewResponseDTO {
    private Integer reviewId;
    private Integer productDetailId;
    private String productName;
    private String colorName;
    private String sizeName;
    private String imageUrl;
    private Integer rating;
    private String comment;
    private List<String> images;
    private String reviewStatus;
    private LocalDateTime createdAt;
    private String customerName;
    private String customerEmail;
    private Long orderId;
}