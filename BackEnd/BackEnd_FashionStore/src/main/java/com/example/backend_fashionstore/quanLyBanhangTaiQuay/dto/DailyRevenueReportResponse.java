package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
public class DailyRevenueReportResponse {
    private LocalDate reportDate;
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal totalDiscount;
    private BigDecimal totalTax;
    private BigDecimal avgOrderValue;
    private Map<String, BigDecimal> paymentMethodStats;
}