package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ReturnItemRequest {
    private Integer orderItemId;
    private Integer productDetailId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPerUnit;   // ✅ Giảm giá trên mỗi đơn vị
    private BigDecimal finalUnitPrice;    // ✅ Giá sau giảm
    private String condition;             // ✅ GOOD hoặc DAMAGED
}