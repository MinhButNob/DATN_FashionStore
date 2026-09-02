package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ExchangeItemRequest {
    private Integer oldProductDetailId;   // ID sản phẩm cũ (có thể null nếu không trả)
    private Integer newProductDetailId;   // ID sản phẩm mới
    private Integer quantity;
    private BigDecimal unitPrice;
    private String newProductName;        // ✅ Thêm tên sản phẩm mới để hiển thị
    private String newColorName;          // ✅ Thêm tên màu sản phẩm mới
    private String newSizeName;           // ✅ Thêm tên size sản phẩm mới
}