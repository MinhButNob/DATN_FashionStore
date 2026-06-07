package com.example.backend_fashionstore.quanLyKhuyenMai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionStatisticsDTO {
    private long totalPromotions;
    private long activePromotions;
    private long inactivePromotions;
}