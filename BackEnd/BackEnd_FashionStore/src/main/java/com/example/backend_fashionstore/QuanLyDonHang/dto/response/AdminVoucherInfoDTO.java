package com.example.backend_fashionstore.QuanLyDonHang.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVoucherInfoDTO {
    private Integer voucherId;
    private String voucherCode;
    private BigDecimal discountAmount;
    private BigDecimal discountPercent;
}