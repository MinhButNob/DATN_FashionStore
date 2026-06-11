package com.example.backend_fashionstore.QuanLySanPham.dto.response;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatsResponse {
    private Long totalProducts;
    private Long inStockVariants;
    private Long lowStockVariants;
    private List<LowStockItem> lowStockItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStockItem {
        private String productName;
        private String sku;
        private Integer currentStock;
        private Integer threshold;
    }
}