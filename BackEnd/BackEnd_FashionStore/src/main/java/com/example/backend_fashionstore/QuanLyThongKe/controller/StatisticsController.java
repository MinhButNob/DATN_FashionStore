// controller/StatisticsController.java
package com.example.backend_fashionstore.QuanLyThongKe.controller;

import com.example.backend_fashionstore.QuanLySanPham.dto.response.ApiResponse;
import com.example.backend_fashionstore.QuanLyThongKe.dto.DashboardStatisticsDTO;
import com.example.backend_fashionstore.QuanLyThongKe.dto.RevenueStatisticsDTO;
import com.example.backend_fashionstore.QuanLyThongKe.dto.DailyRevenueDTO;
import com.example.backend_fashionstore.QuanLyThongKe.dto.TopProductDTO;
import com.example.backend_fashionstore.QuanLyThongKe.dto.StaffPerformanceDTO;
import com.example.backend_fashionstore.QuanLyThongKe.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "http://localhost:5173")
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * Dashboard tổng quan
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatisticsDTO>> getDashboard() {
        log.info("📊 Getting dashboard statistics");
        try {
            DashboardStatisticsDTO data = statisticsService.getDashboardStatistics();
            log.info("✅ Dashboard data: todayRevenue={}, todayOrders={}",
                    data.getTodayRevenue(), data.getTodayOrders());
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            log.error("❌ Error getting dashboard: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("Lỗi khi lấy dữ liệu dashboard: " + e.getMessage()));
        }
    }

    /**
     * Thống kê doanh thu theo khoảng thời gian
     */
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueStatisticsDTO>> getRevenueStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        log.info("📊 Getting revenue statistics from {} to {}", fromDate, toDate);
        try {
            RevenueStatisticsDTO data = statisticsService.getRevenueStatistics(fromDate, toDate);
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            log.error("❌ Error getting revenue statistics: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("Lỗi khi lấy dữ liệu: " + e.getMessage()));
        }
    }

    /**
     * Thống kê doanh thu theo ngày
     */
    @GetMapping("/daily-revenue")
    public ResponseEntity<ApiResponse<List<DailyRevenueDTO>>> getDailyRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        log.info("📊 Getting daily revenue from {} to {}", fromDate, toDate);
        try {
            List<DailyRevenueDTO> data = statisticsService.getDailyRevenue(fromDate, toDate);
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            log.error("❌ Error getting daily revenue: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("Lỗi khi lấy dữ liệu: " + e.getMessage()));
        }
    }

    /**
     * Sản phẩm bán chạy
     */
    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<List<TopProductDTO>>> getTopProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("📊 Getting top {} products from {} to {}", limit, fromDate, toDate);
        try {
            LocalDateTime start = fromDate.atStartOfDay();
            LocalDateTime end = toDate.atTime(23, 59, 59);
            List<TopProductDTO> data = statisticsService.getTopProducts(start, end, limit);
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            log.error("❌ Error getting top products: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("Lỗi khi lấy dữ liệu: " + e.getMessage()));
        }
    }

    /**
     * Nhân viên xuất sắc
     */
    @GetMapping("/top-staff")
    public ResponseEntity<ApiResponse<List<StaffPerformanceDTO>>> getTopStaff(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("📊 Getting top {} staff from {} to {}", limit, fromDate, toDate);
        try {
            LocalDateTime start = fromDate != null ? fromDate.atStartOfDay() : LocalDate.of(2000, 1, 1).atStartOfDay();
            LocalDateTime end = toDate != null ? toDate.atTime(23, 59, 59) : LocalDate.of(2099, 12, 31).atTime(23, 59, 59);
            List<StaffPerformanceDTO> data = statisticsService.getTopStaff(start, end, limit);
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            log.error("❌ Error getting top staff: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("Lỗi khi lấy dữ liệu: " + e.getMessage()));
        }
    }
}