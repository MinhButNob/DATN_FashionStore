// DashboardService.java
package com.example.backend_fashionstore.QuanLyThongKe.service;

import com.example.backend_fashionstore.QuanLyThongKe.dto.DashboardResponseDTO;
import com.example.backend_fashionstore.QuanLyThongKe.dto.TopProductDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface DashboardService {

    /**
     * Lấy toàn bộ dữ liệu Dashboard
     */
    DashboardResponseDTO getDashboardData();

    /**
     * Lấy doanh thu theo tháng trong năm
     */
    Map<Integer, BigDecimal> getMonthlyRevenue(int year);

    /**
     * Lấy top sản phẩm bán chạy
     */
    List<TopProductDTO> getTopProducts(int limit);

    /**
     * Lấy tăng trưởng doanh thu (so với tháng trước)
     */
    BigDecimal getRevenueGrowth();
}