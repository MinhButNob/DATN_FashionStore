package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ExchangeItemRequest {
    private Integer oldProductDetailId;
    private Integer newProductDetailId;
    private Integer quantity;
    private BigDecimal unitPrice;
}