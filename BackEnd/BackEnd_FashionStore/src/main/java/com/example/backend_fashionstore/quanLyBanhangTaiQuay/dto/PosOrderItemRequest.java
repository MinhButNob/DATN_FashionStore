package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PosOrderItemRequest {
    private Integer productDetailId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;  // Giảm giá từng sản phẩm
    private String notes;
    private String condition;     // ✅ Thêm tình trạng sản phẩm: GOOD, DAMAGED
}