// DashboardResponseDTO.java
package com.example.backend_fashionstore.QuanLyThongKe.dto;

import com.example.backend_fashionstore.QuanLyDonHang.dto.OrderStatisticsDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.AdminOrderResponseDTO;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDTO {
    // Thống kê đơn hàng
    private OrderStatisticsDTO orderStatistics;

    // Thống kê sản phẩm
    private ProductStatisticsDTO productStatistics;

    // Thống kê khách hàng
    private CustomerStatisticsDTO customerStatistics;

    // Doanh thu theo tháng
    private Map<Integer, BigDecimal> monthlyRevenue;

    // Top sản phẩm bán chạy
    private List<TopProductDTO> topProducts;

    // Đơn hàng gần đây
    private List<AdminOrderResponseDTO> recentOrders;

    // Tăng trưởng doanh thu
    private BigDecimal revenueGrowth;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductStatisticsDTO {
        private Long totalProducts;
        private Long inStockVariants;
        private Long lowStockVariants;
        private Long deletedProducts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerStatisticsDTO {
        private Long totalCustomers;
        private Long activeCustomers;
        private Long lockedCustomers;
        private Long deletedCustomers;
    }
}