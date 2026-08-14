package com.example.backend_fashionstore.QuanLySanPham.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Integer productId;
    private String productName;
    private String description;
    private Double weight;
    private String status;
    private Integer totalStock;
    private LocalDateTime createdAt;
    private String mainImageUrl;
    private CategorySummary category;
    private List<VariantSummary> variants;
    private PromotionSummary promotion;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private Integer categoryId;
        private String categoryName;
    }

    // ✅ THÊM promotion summary class
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromotionSummary {
        private Integer promotionId;
        private String promotionName;
        private BigDecimal promotionDiscountPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantSummary {
        private Integer productDetailId;
        private String sku;
        private String color;
        private String size;
        private BigDecimal price;
        private Integer quantity;
        private String status;
        private Boolean isLowStock;
        private BigDecimal discountedPrice; // ✅ THÊM giá sau khuyến mãi
        private List<PromotionSummary> promotions;
    }
}
