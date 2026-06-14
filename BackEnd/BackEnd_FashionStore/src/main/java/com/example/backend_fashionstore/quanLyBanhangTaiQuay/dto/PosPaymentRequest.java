package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PosPaymentRequest {
    private Integer paymentMethodId;  // 1: COD, 2: VNPAY, 3: MOMO
    private BigDecimal amount;
    private String referenceNumber;
}