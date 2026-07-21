// RevenueController.java
package com.example.backend_fashionstore.QuanLyThongKe.controller;

import com.example.backend_fashionstore.QuanLySanPham.dto.response.ApiResponse;
import com.example.backend_fashionstore.QuanLyThongKe.dto.RevenueStatisticsDTO;
import com.example.backend_fashionstore.QuanLyThongKe.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/revenue")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@CrossOrigin(origins = "http://localhost:5173")
public class RevenueController {

    private final StatisticsService statisticsService;

    /**
     * Lấy tổng doanh thu từ tất cả đơn hàng COMPLETED
     */
    @GetMapping("/total")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTotalRevenue() {
        log.info("💰 Getting total revenue from all completed orders");
        try {
            BigDecimal totalRevenue = statisticsService.getTotalRevenueAllTime();
            Long totalOrders = statisticsService.getTotalCompletedOrdersAllTime();
            BigDecimal avgOrderValue = statisticsService.getAverageOrderValueAllTime();

            Map<String, Object> data = new HashMap<>();
            data.put("totalRevenue", totalRevenue);
            data.put("totalOrders", totalOrders);
            data.put("averageOrderValue", avgOrderValue);

            log.info("💰 Total revenue: {}, Total orders: {}, Avg order value: {}",
                    totalRevenue, totalOrders, avgOrderValue);

            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            log.error("❌ Error getting total revenue: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("Lỗi khi lấy tổng doanh thu: " + e.getMessage()));
        }
    }

    /**
     * Lấy tổng doanh thu theo khoảng thời gian
     */
    @GetMapping("/total/between")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTotalRevenueBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        log.info("💰 Getting total revenue from {} to {}", fromDate, toDate);
        try {
            BigDecimal totalRevenue = statisticsService.getTotalRevenueBetween(fromDate, toDate);

            Map<String, Object> data = new HashMap<>();
            data.put("totalRevenue", totalRevenue);
            data.put("fromDate", fromDate);
            data.put("toDate", toDate);

            log.info("💰 Total revenue from {} to {}: {}", fromDate, toDate, totalRevenue);

            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            log.error("❌ Error getting total revenue between: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("Lỗi khi lấy tổng doanh thu: " + e.getMessage()));
        }
    }
}