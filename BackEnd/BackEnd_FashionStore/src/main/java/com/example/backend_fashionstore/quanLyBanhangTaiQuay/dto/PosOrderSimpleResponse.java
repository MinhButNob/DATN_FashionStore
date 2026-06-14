package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PosOrderSimpleResponse {
    private Integer orderId;
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}