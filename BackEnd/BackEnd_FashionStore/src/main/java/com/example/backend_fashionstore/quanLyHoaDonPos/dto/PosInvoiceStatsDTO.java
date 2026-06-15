package com.example.backend_fashionstore.quanLyHoaDonPos.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PosInvoiceStatsDTO {
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal avgOrderValue;
    private Long totalCustomers;
    private BigDecimal totalDiscount;
    private BigDecimal totalTax;
    private LocalDate fromDate;
    private LocalDate toDate;
}