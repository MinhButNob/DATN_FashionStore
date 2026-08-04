package com.example.backend_fashionstore.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewStatDTO {
    private Integer productId;
    private String productName;
    private String productImageUrl;
    private Long totalReviews;
    private Double averageRating;
}
