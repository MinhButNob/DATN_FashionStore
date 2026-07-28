// dto/statistics/StaffPerformanceDTO.java
package com.example.backend_fashionstore.QuanLyThongKe.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffPerformanceDTO {
    private Integer staffId;
    private String staffName;
    private String staffRole;
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private Long totalCustomers;
}