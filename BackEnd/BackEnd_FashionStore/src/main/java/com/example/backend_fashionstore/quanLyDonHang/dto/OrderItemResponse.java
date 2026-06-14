package com.example.backend_fashionstore.quanLyDonHang.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Long id;
    private String name;
    private String image;
    private BigDecimal price;
    private Integer quantity;
    private String size;
    private String color;
}
