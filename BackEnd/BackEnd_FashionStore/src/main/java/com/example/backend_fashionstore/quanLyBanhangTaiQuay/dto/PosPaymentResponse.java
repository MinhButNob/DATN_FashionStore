package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PosPaymentResponse {
    private Integer posPaymentId;
    private Integer paymentMethodId;
    private String paymentMethodName;
    private BigDecimal amount;
    private String referenceNumber;
    private String status;
    private LocalDateTime processedAt;
    /** Trả về khi tạo link VNPay tại quầy; null với tiền mặt */
    private String paymentUrl;
    /** Mã txnRef VNPay của phiên này; null với tiền mặt */
    private String txnRef;
}