// dto/DashboardStatisticsDTO.java
package com.example.backend_fashionstore.QuanLyThongKe.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatisticsDTO {
    // Tổng quan
    private BigDecimal todayRevenue;
    private Long todayOrders;
    private BigDecimal thisWeekRevenue;
    private Long thisWeekOrders;
    private BigDecimal thisMonthRevenue;
    private Long thisMonthOrders;

    // Tăng trưởng
    private BigDecimal revenueGrowth;
    private BigDecimal orderGrowth;

    // Phân loại
    private BigDecimal onlineRevenue;
    private Long onlineOrders;
    private BigDecimal posRevenue;
    private Long posOrders;

    // Khách hàng
    private Long totalCustomers;

    // ✅ TỔNG DOANH THU TÍCH LŨY (ALL TIME)
    private BigDecimal totalRevenueAllTime;
    private BigDecimal onlineRevenueAllTime;
    private BigDecimal posRevenueAllTime;
    private Long totalOrdersAllTime;

    // Dữ liệu chi tiết
    private List<DailyRevenueDTO> dailyRevenue;
    private List<TopProductDTO> topProducts;
    private List<StaffPerformanceDTO> topStaff;
}