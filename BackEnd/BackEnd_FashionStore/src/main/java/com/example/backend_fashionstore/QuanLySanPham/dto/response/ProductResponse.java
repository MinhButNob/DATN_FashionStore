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
    private String status;
    private Integer totalStock;
    private LocalDateTime createdAt;

    private CategorySummary category;
    private List<VariantSummary> variants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private Integer categoryId;
        private String categoryName;
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
    }
}