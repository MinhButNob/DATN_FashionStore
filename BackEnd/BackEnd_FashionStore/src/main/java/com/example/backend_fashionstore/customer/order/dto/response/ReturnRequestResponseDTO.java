package com.example.backend_fashionstore.customer.order.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestResponseDTO {
    private Long returnRequestId;
    private Long orderId;
    private String orderCode;
    private List<Integer> productDetailIds;
    private Map<Integer, Integer> quantities;
    private List<ReturnItemDTO> returnItems;
    private String reason;
    private String description;
    private List<String> images;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String adminNote;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemDTO {
        private Integer productDetailId;
        private String productName;
        private String colorName;
        private String sizeName;
        private String imageUrl;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal refundAmount;
    }
}