package com.example.backend_fashionstore.QuanLyThongKe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAlertDTO {
    private Integer productDetailId;
    private String productName;
    private String sizeName;
    private String colorName;
    private Integer quantity;
    private String imageUrl;
}
