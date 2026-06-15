package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class MonthlyRevenueReportResponse {
    private Integer year;
    private Integer month;
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal totalDiscount;
    private BigDecimal totalTax;
    private BigDecimal avgOrderValue;
    private Map<String, BigDecimal> paymentMethodStats;
    private Map<Integer, DailyRevenueReportResponse> dailyReports;
}