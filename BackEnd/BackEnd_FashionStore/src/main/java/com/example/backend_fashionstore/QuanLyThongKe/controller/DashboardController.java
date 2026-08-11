// DashboardController.java
package com.example.backend_fashionstore.QuanLyThongKe.controller;

import com.example.backend_fashionstore.QuanLyThongKe.dto.DashboardResponseDTO;
import com.example.backend_fashionstore.QuanLyThongKe.dto.TopProductDTO;
import com.example.backend_fashionstore.QuanLyThongKe.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@CrossOrigin(origins = "http://localhost:5173")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Lấy toàn bộ dữ liệu Dashboard
     */
    @GetMapping
    public ResponseEntity<DashboardResponseDTO> getDashboard() {
        log.info("📊 Getting dashboard data");
        DashboardResponseDTO data = dashboardService.getDashboardData();
        return ResponseEntity.ok(data);
    }

    /**
     * Lấy doanh thu theo tháng trong năm
     */
    @GetMapping("/revenue/monthly")
    public ResponseEntity<Map<Integer, BigDecimal>> getMonthlyRevenue(
            @RequestParam(defaultValue = "#{T(java.time.LocalDateTime).now().getYear()}") int year
    ) {
        log.info("📊 Getting monthly revenue for year: {}", year);
        Map<Integer, BigDecimal> monthlyRevenue = dashboardService.getMonthlyRevenue(year);
        return ResponseEntity.ok(monthlyRevenue);
    }

    /**
     * Lấy top sản phẩm bán chạy
     */
    @GetMapping("/top-products")
    public ResponseEntity<List<TopProductDTO>> getTopProducts(
            @RequestParam(defaultValue = "5") int limit
    ) {
        log.info("🏆 Getting top {} products", limit);
        List<TopProductDTO> topProducts = dashboardService.getTopProducts(limit);
        return ResponseEntity.ok(topProducts);
    }

    /**
     * Lấy tăng trưởng doanh thu
     */
    @GetMapping("/revenue/growth")
    public ResponseEntity<BigDecimal> getRevenueGrowth() {
        log.info("📈 Getting revenue growth");
        BigDecimal growth = dashboardService.getRevenueGrowth();
        return ResponseEntity.ok(growth);
    }

    /**
     * Lấy doanh thu theo ngày trong tháng
     */
    @GetMapping("/revenue/daily")
    public ResponseEntity<Map<Integer, BigDecimal>> getDailyRevenue(
            @RequestParam(defaultValue = "#{T(java.time.LocalDateTime).now().getYear()}") int year,
            @RequestParam(defaultValue = "#{T(java.time.LocalDateTime).now().getMonthValue()}") int month
    ) {
        log.info("📊 Getting daily revenue for year: {}, month: {}", year, month);
        Map<Integer, BigDecimal> dailyRevenue = dashboardService.getDailyRevenue(year, month);
        return ResponseEntity.ok(dailyRevenue);
    }

    /**
     * Lấy thống kê trạng thái đơn hàng (Pie Chart)
     */
    @GetMapping("/order-status")
    public ResponseEntity<Map<String, Long>> getOrderStatusStats(
            @RequestParam(defaultValue = "Ngày") String filterType,
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer year
    ) {
        log.info("📊 Getting order status stats");
        Map<String, Long> stats = dashboardService.getOrderStatusStats(filterType, dateRange, month, year);
        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy danh sách sản phẩm sắp hết hàng
     */
    @GetMapping("/inventory-alerts")
    public ResponseEntity<List<com.example.backend_fashionstore.QuanLyThongKe.dto.InventoryAlertDTO>> getInventoryAlerts(
            @RequestParam(defaultValue = "10") int threshold,
            @RequestParam(defaultValue = "5") int limit
    ) {
        log.info("⚠️ Getting inventory alerts");
        List<com.example.backend_fashionstore.QuanLyThongKe.dto.InventoryAlertDTO> alerts = dashboardService.getInventoryAlerts(threshold, limit);
        return ResponseEntity.ok(alerts);
    }
}