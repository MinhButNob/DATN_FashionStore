package com.example.backend_fashionstore.quanLyHoaDonPos.dto;


import lombok.Data;
import java.math.BigDecimal;

@Data
public class PosInvoiceItemDTO {
    private Integer productDetailId;
    private String productName;
    private String color;
    private String size;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal total;
}