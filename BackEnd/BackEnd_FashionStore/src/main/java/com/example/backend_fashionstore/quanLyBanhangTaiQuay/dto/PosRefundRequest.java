package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PosRefundRequest {
    private Integer orderId;
    private BigDecimal amount;
    private String reason;
    private Integer refundMethodId;
}