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

    /**
     * Lấy doanh thu theo ngày trong tháng
     */
    Map<Integer, BigDecimal> getDailyRevenue(int year, int month);

    /**
     * Lấy thống kê trạng thái đơn hàng
     */
    Map<String, Long> getOrderStatusStats(String filterType, String dateRange, String month, Integer year);

    /**
     * Lấy danh sách sản phẩm sắp hết hàng
     */
    List<com.example.backend_fashionstore.QuanLyThongKe.dto.InventoryAlertDTO> getInventoryAlerts(int threshold, int limit);
}