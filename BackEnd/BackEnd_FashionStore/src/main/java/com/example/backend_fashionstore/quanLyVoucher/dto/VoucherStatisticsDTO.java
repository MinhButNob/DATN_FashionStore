package com.example.backend_fashionstore.quanLyVoucher.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherStatisticsDTO {
    private long totalVouchers;
    private long activeVouchers;
    private long inactiveVouchers;
    private long totalUsedCount;
}