package com.example.backend_fashionstore.quanLyHoaDonPos.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PosInvoicePaymentDTO {
    private String paymentMethod;
    private BigDecimal amount;
    private String referenceNumber;
    private LocalDateTime processedAt;
}