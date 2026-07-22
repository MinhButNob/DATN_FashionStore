package com.example.backend_fashionstore.QuanLyDonHang.dto.response;

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
public class ReturnRequestDetailDTO {
    private Long returnRequestId;
    private Long orderId;
    private String orderCode;
    private List<Integer> productDetailIds;
    private Map<Integer, Integer> quantities;
    private List<ReturnItemDetailDTO> returnItems;
    private String reason;
    private String description;
    private List<String> images;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String adminNote;
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemDetailDTO {
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