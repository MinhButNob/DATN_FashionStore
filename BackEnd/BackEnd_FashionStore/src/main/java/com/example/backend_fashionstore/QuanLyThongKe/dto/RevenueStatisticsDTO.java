// dto/statistics/RevenueStatisticsDTO.java
package com.example.backend_fashionstore.QuanLyThongKe.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatisticsDTO {
    // Tổng quan (theo khoảng thời gian)
    private BigDecimal totalRevenue;
    private Long totalOrders;
    private Long totalCustomers;
    private BigDecimal averageOrderValue;

    // Phân loại theo kênh bán hàng (theo khoảng thời gian)
    private BigDecimal onlineRevenue;
    private Long onlineOrders;
    private BigDecimal posRevenue;
    private Long posOrders;

    // ✅ TỔNG DOANH THU TÍCH LŨY (ALL TIME)
    private BigDecimal totalRevenueAllTime;
    private Long totalOrdersAllTime;

    // Thông tin thời gian
    private LocalDate fromDate;
    private LocalDate toDate;
}