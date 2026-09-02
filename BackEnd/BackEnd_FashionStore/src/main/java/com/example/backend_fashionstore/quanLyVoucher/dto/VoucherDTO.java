package com.example.backend_fashionstore.quanLyVoucher.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherDTO {
    private Integer voucherId;
    private String voucherCode;
    private BigDecimal voucherDiscountAmount;
    private BigDecimal voucherDiscountPercent;
    private BigDecimal voucherMaxDiscountAmount; // ✅ THÊM
    private LocalDateTime voucherStartDate;
    private LocalDateTime voucherEndDate;
    private Integer voucherMaxUsage;
    private Integer voucherUsedCount;
    private BigDecimal voucherMinOrderValue;
    private String voucherStatus;
    private LocalDateTime voucherCreatedAt;
    private LocalDateTime voucherUpdatedAt;
    private LocalDateTime voucherDeletedAt;
}