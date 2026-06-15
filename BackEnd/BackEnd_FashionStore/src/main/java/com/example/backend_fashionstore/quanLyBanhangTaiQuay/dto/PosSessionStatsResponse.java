package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PosSessionStatsResponse {
    private Integer sessionId;
    private Integer cashRegisterId;
    private Integer employeeId;
    private LocalDateTime startTime;
    private BigDecimal startBalance;
    private Integer totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal avgOrderValue;
    private BigDecimal cashSales;
    private BigDecimal cardSales;
    private BigDecimal transferSales;
    private BigDecimal totalSales;
}