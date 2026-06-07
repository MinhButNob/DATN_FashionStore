package com.example.backend_fashionstore.quanLyKhuyenMai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDTO {
    private Integer promotionId;
    private String promotionName;
    private String promotionDescription;
    private LocalDateTime promotionStartDate;
    private LocalDateTime promotionEndDate;
    private BigDecimal promotionDiscountPercent;
    private String promotionStatus;
    private LocalDateTime promotionCreatedAt;
    private LocalDateTime promotionUpdatedAt;
    private LocalDateTime promotionDeletedAt;
}