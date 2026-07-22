// dto/statistics/DailyRevenueDTO.java
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
public class DailyRevenueDTO {
    private LocalDate date;
    private BigDecimal onlineRevenue;
    private Long onlineOrders;
    private BigDecimal posRevenue;
    private Long posOrders;
    private BigDecimal totalRevenue;
    private Long totalOrders;
}