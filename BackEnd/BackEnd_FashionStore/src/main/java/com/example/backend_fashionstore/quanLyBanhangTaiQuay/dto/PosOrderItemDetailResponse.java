package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PosOrderItemDetailResponse {
    private Integer posOrderItemId;
    private String productName;
    private String colorName;
    private String sizeName;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal total;

    // ✅ Thêm các trường cho hoàn hàng
    private BigDecimal discountPerUnit;   // Giảm giá trên mỗi đơn vị
    private BigDecimal finalUnitPrice;    // Giá sau giảm
}