// service/StatisticsService.java - THÊM CÁC PHƯƠNG THỨC MỚI
package com.example.backend_fashionstore.QuanLyThongKe.service;

import com.example.backend_fashionstore.QuanLyThongKe.dto.*;
import com.example.backend_fashionstore.QuanLyThongKe.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatisticsRepository statisticsRepository;

    private static final int TOP_PRODUCT_LIMIT = 10;
    private static final int TOP_STAFF_LIMIT = 10;

    // =========================================
    // ✅ THÊM MỚI: TỔNG DOANH THU TỪ TẤT CẢ ĐƠN HÀNG
    // =========================================

    /**
     * Lấy tổng doanh thu từ tất cả đơn hàng COMPLETED
     */
    public BigDecimal getTotalRevenueAllTime() {
        log.info("💰 Getting total revenue from all completed orders");
        try {
            BigDecimal onlineRevenue = statisticsRepository.getTotalOnlineRevenueAllTime();
            BigDecimal posRevenue = statisticsRepository.getTotalPosRevenueAllTime();
            BigDecimal total = onlineRevenue.add(posRevenue);
            log.info("💰 Total revenue all time: Online={}, POS={}, Total={}",
                    onlineRevenue, posRevenue, total);
            return total;
        } catch (Exception e) {
            log.error("❌ Error getting total revenue: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Lấy tổng doanh thu từ tất cả đơn hàng COMPLETED theo khoảng thời gian
     */
    public BigDecimal getTotalRevenueBetween(LocalDate fromDate, LocalDate toDate) {
        log.info("💰 Getting total revenue from {} to {}", fromDate, toDate);
        try {
            LocalDateTime start = fromDate.atStartOfDay();
            LocalDateTime end = toDate.atTime(LocalTime.MAX);

            BigDecimal onlineRevenue = statisticsRepository.getOnlineRevenue(start, end);
            BigDecimal posRevenue = statisticsRepository.getPosRevenue(start, end);
            BigDecimal total = onlineRevenue.add(posRevenue);

            log.info("💰 Total revenue from {} to {}: Online={}, POS={}, Total={}",
                    fromDate, toDate, onlineRevenue, posRevenue, total);
            return total;
        } catch (Exception e) {
            log.error("❌ Error getting total revenue between: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Lấy tổng số đơn hàng COMPLETED
     */
    public Long getTotalCompletedOrdersAllTime() {
        log.info("📦 Getting total completed orders count");
        try {
            Long onlineOrders = statisticsRepository.getTotalOnlineOrdersAllTime();
            Long posOrders = statisticsRepository.getTotalPosOrdersAllTime();
            Long total = (onlineOrders != null ? onlineOrders : 0L) + (posOrders != null ? posOrders : 0L);
            log.info("📦 Total completed orders: Online={}, POS={}, Total={}",
                    onlineOrders, posOrders, total);
            return total;
        } catch (Exception e) {
            log.error("❌ Error getting total completed orders: {}", e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * Lấy giá trị trung bình đơn hàng COMPLETED
     */
    public BigDecimal getAverageOrderValueAllTime() {
        log.info("📊 Getting average order value");
        try {
            BigDecimal avg = statisticsRepository.getAverageOrderValueAllTime();
            log.info("📊 Average order value: {}", avg);
            return avg != null ? avg : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("❌ Error getting average order value: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    // =========================================
    // PHƯƠNG THỨC HIỆN CÓ (GIỮ NGUYÊN)
    // =========================================

    /**
     * Lấy thống kê tổng quan Dashboard
     */
    public DashboardStatisticsDTO getDashboardStatistics() {
        log.info("📊 Getting dashboard statistics...");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime todayStart = now.with(LocalTime.MIN);
            LocalDateTime todayEnd = now.with(LocalTime.MAX);

            LocalDateTime weekStart = now.minusDays(7).with(LocalTime.MIN);
            LocalDateTime monthStart = now.withDayOfMonth(1).with(LocalTime.MIN);
            LocalDateTime lastMonthStart = now.minusMonths(1).withDayOfMonth(1).with(LocalTime.MIN);
            LocalDateTime lastMonthEnd = now.withDayOfMonth(1).minusDays(1).with(LocalTime.MAX);

            // ===== DOANH THU HÔM NAY =====
            BigDecimal todayOnlineRevenue = getSafeValue(() -> statisticsRepository.getTodayOnlineRevenue());
            Long todayOnlineOrders = getSafeLong(() -> statisticsRepository.getTodayOnlineOrders());
            BigDecimal todayPosRevenue = getSafeValue(() -> statisticsRepository.getTodayPosRevenue());
            Long todayPosOrders = getSafeLong(() -> statisticsRepository.getTodayPosOrders());

            BigDecimal todayRevenue = todayOnlineRevenue.add(todayPosRevenue);
            Long todayOrders = todayOnlineOrders + todayPosOrders;

            // ===== DOANH THU TUẦN NÀY =====
            BigDecimal weekOnlineRevenue = getSafeValue(() -> statisticsRepository.getOnlineRevenueBetween(weekStart, todayEnd));
            Long weekOnlineOrders = getSafeLong(() -> statisticsRepository.getOnlineOrderCount(weekStart, todayEnd));
            BigDecimal weekPosRevenue = getSafeValue(() -> statisticsRepository.getPosRevenueBetween(weekStart, todayEnd));
            Long weekPosOrders = getSafeLong(() -> statisticsRepository.getPosOrderCount(weekStart, todayEnd));

            BigDecimal weekRevenue = weekOnlineRevenue.add(weekPosRevenue);
            Long weekOrders = weekOnlineOrders + weekPosOrders;

            // ===== DOANH THU THÁNG NÀY =====
            BigDecimal monthOnlineRevenue = getSafeValue(() -> statisticsRepository.getOnlineRevenueBetween(monthStart, todayEnd));
            Long monthOnlineOrders = getSafeLong(() -> statisticsRepository.getOnlineOrderCount(monthStart, todayEnd));
            BigDecimal monthPosRevenue = getSafeValue(() -> statisticsRepository.getPosRevenueBetween(monthStart, todayEnd));
            Long monthPosOrders = getSafeLong(() -> statisticsRepository.getPosOrderCount(monthStart, todayEnd));

            BigDecimal monthRevenue = monthOnlineRevenue.add(monthPosRevenue);
            Long monthOrders = monthOnlineOrders + monthPosOrders;

            // ===== TĂNG TRƯỞNG =====
            BigDecimal lastMonthOnlineRevenue = getSafeValue(() -> statisticsRepository.getOnlineRevenueBetween(lastMonthStart, lastMonthEnd));
            Long lastMonthOnlineOrders = getSafeLong(() -> statisticsRepository.getOnlineOrderCount(lastMonthStart, lastMonthEnd));
            BigDecimal lastMonthPosRevenue = getSafeValue(() -> statisticsRepository.getPosRevenueBetween(lastMonthStart, lastMonthEnd));
            Long lastMonthPosOrders = getSafeLong(() -> statisticsRepository.getPosOrderCount(lastMonthStart, lastMonthEnd));

            BigDecimal lastMonthRevenue = lastMonthOnlineRevenue.add(lastMonthPosRevenue);
            Long lastMonthOrders = lastMonthOnlineOrders + lastMonthPosOrders;

            BigDecimal revenueGrowth = BigDecimal.ZERO;
            BigDecimal orderGrowth = BigDecimal.ZERO;

            if (lastMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
                revenueGrowth = monthRevenue.subtract(lastMonthRevenue)
                        .divide(lastMonthRevenue, 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            if (lastMonthOrders > 0) {
                orderGrowth = BigDecimal.valueOf(monthOrders - lastMonthOrders)
                        .divide(BigDecimal.valueOf(lastMonthOrders), 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            // ===== DOANH THU THEO NGÀY =====
            List<DailyRevenueDTO> dailyRevenue = getDailyRevenue(weekStart.toLocalDate(), todayEnd.toLocalDate());

            // ===== SẢN PHẨM BÁN CHẠY =====
            List<TopProductDTO> topProducts = getTopProducts(monthStart, todayEnd, TOP_PRODUCT_LIMIT);

            // ===== NHÂN VIÊN XUẤT SẮC =====
            List<StaffPerformanceDTO> topStaff = getTopStaff(monthStart, todayEnd, TOP_STAFF_LIMIT);

            // ===== TỔNG KHÁCH HÀNG =====
            Long totalCustomers = getSafeLong(() -> statisticsRepository.getOnlineCustomerCount(monthStart, todayEnd));

            // ===== TỔNG DOANH THU TÍCH LŨY (ALL TIME) =====
            BigDecimal totalOnlineRevenueAllTime = getSafeValue(() -> statisticsRepository.getTotalOnlineRevenueAllTime());
            BigDecimal totalPosRevenueAllTime = getSafeValue(() -> statisticsRepository.getTotalPosRevenueAllTime());
            Long totalOnlineOrdersAllTime = getSafeLong(() -> statisticsRepository.getTotalOnlineOrdersAllTime());
            Long totalPosOrdersAllTime = getSafeLong(() -> statisticsRepository.getTotalPosOrdersAllTime());

            BigDecimal totalRevenueAllTime = totalOnlineRevenueAllTime.add(totalPosRevenueAllTime);
            Long totalOrdersAllTime = totalOnlineOrdersAllTime + totalPosOrdersAllTime;

            log.info("📊 All Time: Total Revenue={}, Total Orders={}", totalRevenueAllTime, totalOrdersAllTime);

            return DashboardStatisticsDTO.builder()
                    .todayRevenue(todayRevenue)
                    .todayOrders(todayOrders)
                    .thisWeekRevenue(weekRevenue)
                    .thisWeekOrders(weekOrders)
                    .thisMonthRevenue(monthRevenue)
                    .thisMonthOrders(monthOrders)
                    .revenueGrowth(revenueGrowth)
                    .orderGrowth(orderGrowth)
                    .onlineRevenue(monthOnlineRevenue)
                    .onlineOrders(monthOnlineOrders)
                    .posRevenue(monthPosRevenue)
                    .posOrders(monthPosOrders)
                    .totalCustomers(totalCustomers)
                    .totalRevenueAllTime(totalRevenueAllTime)
                    .onlineRevenueAllTime(totalOnlineRevenueAllTime)
                    .posRevenueAllTime(totalPosRevenueAllTime)
                    .totalOrdersAllTime(totalOrdersAllTime)
                    .dailyRevenue(dailyRevenue)
                    .topProducts(topProducts)
                    .topStaff(topStaff)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error getting dashboard statistics: {}", e.getMessage(), e);
            return getEmptyDashboardStatistics();
        }
    }

    /**
     * Lấy thống kê doanh thu theo ngày
     */
    public List<DailyRevenueDTO> getDailyRevenue(LocalDate fromDate, LocalDate toDate) {
        log.info("📊 Getting daily revenue from {} to {}", fromDate, toDate);

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);

        List<DailyRevenueDTO> result = new ArrayList<>();

        try {
            List<Object[]> onlineData = statisticsRepository.getDailyOnlineRevenue(start, end);
            List<Object[]> posData = statisticsRepository.getDailyPosRevenue(start, end);

            // Map dữ liệu online
            Map<LocalDate, BigDecimal[]> onlineMap = new HashMap<>();
            if (onlineData != null) {
                for (Object[] row : onlineData) {
                    if (row != null && row.length >= 3) {
                        LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
                        BigDecimal revenue = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                        Long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                        onlineMap.put(date, new BigDecimal[]{revenue, BigDecimal.valueOf(count)});
                    }
                }
            }

            // Map dữ liệu POS
            Map<LocalDate, BigDecimal[]> posMap = new HashMap<>();
            if (posData != null) {
                for (Object[] row : posData) {
                    if (row != null && row.length >= 3) {
                        LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
                        BigDecimal revenue = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                        Long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                        posMap.put(date, new BigDecimal[]{revenue, BigDecimal.valueOf(count)});
                    }
                }
            }

            // Tổng hợp dữ liệu theo từng ngày
            LocalDate current = fromDate;
            while (!current.isAfter(toDate)) {
                BigDecimal[] online = onlineMap.getOrDefault(current, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                BigDecimal[] pos = posMap.getOrDefault(current, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});

                result.add(DailyRevenueDTO.builder()
                        .date(current)
                        .onlineRevenue(online[0])
                        .onlineOrders(online[1].longValue())
                        .posRevenue(pos[0])
                        .posOrders(pos[1].longValue())
                        .totalRevenue(online[0].add(pos[0]))
                        .totalOrders(online[1].longValue() + pos[1].longValue())
                        .build());

                current = current.plusDays(1);
            }

        } catch (Exception e) {
            log.error("❌ Error getting daily revenue: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Lấy sản phẩm bán chạy (từ cả Online và POS)
     */
    public List<TopProductDTO> getTopProducts(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        log.info("📊 Getting top {} products from {} to {}", limit, startDate, endDate);

        Map<Integer, TopProductDTO> productMap = new HashMap<>();

        try {
            // Lấy sản phẩm bán chạy từ đơn hàng Online
            List<Object[]> onlineProducts = statisticsRepository.getTopSellingProducts(startDate, endDate, limit);
            if (onlineProducts != null) {
                for (Object[] row : onlineProducts) {
                    if (row != null && row.length >= 5) {
                        Integer productId = ((Number) row[0]).intValue();
                        TopProductDTO dto = productMap.get(productId);
                        if (dto == null) {
                            dto = TopProductDTO.builder()
                                    .productId(productId)
                                    .productName(row[1] != null ? (String) row[1] : "Unknown")
                                    .totalSold(0L)
                                    .revenue(BigDecimal.ZERO)
                                    .imageUrl(null)
                                    .build();
                            productMap.put(productId, dto);
                        }
                        dto.setTotalSold(dto.getTotalSold() + ((Number) row[3]).longValue());
                        dto.setRevenue(dto.getRevenue().add(row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO));
                    }
                }
            }

            // Lấy sản phẩm bán chạy từ POS
            List<Object[]> posProducts = statisticsRepository.getTopSellingPosProducts(startDate, endDate, limit);
            if (posProducts != null) {
                for (Object[] row : posProducts) {
                    if (row != null && row.length >= 5) {
                        Integer productId = ((Number) row[0]).intValue();
                        TopProductDTO dto = productMap.get(productId);
                        if (dto == null) {
                            dto = TopProductDTO.builder()
                                    .productId(productId)
                                    .productName(row[1] != null ? (String) row[1] : "Unknown")
                                    .totalSold(0L)
                                    .revenue(BigDecimal.ZERO)
                                    .imageUrl(null)
                                    .build();
                            productMap.put(productId, dto);
                        }
                        dto.setTotalSold(dto.getTotalSold() + ((Number) row[3]).longValue());
                        dto.setRevenue(dto.getRevenue().add(row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO));
                    }
                }
            }

        } catch (Exception e) {
            log.error("❌ Error getting top products: {}", e.getMessage(), e);
        }

        // Sắp xếp theo doanh thu giảm dần và lấy top limit
        return productMap.values().stream()
                .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Lấy nhân viên xuất sắc (từ POS)
     */
    public List<StaffPerformanceDTO> getTopStaff(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        log.info("📊 Getting top {} staff from {} to {}", limit, startDate, endDate);

        List<StaffPerformanceDTO> result = new ArrayList<>();

        try {
            List<Object[]> staffData = statisticsRepository.getTopPosStaff(startDate, endDate, limit);

            if (staffData != null) {
                for (Object[] row : staffData) {
                    if (row != null && row.length >= 4) {
                        Integer staffId = ((Number) row[0]).intValue();
                        String staffName = row[1] != null ? (String) row[1] : "Unknown";
                        Long totalOrders = ((Number) row[2]).longValue();
                        BigDecimal totalRevenue = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;

                        BigDecimal avgOrderValue = totalOrders > 0
                                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                        result.add(StaffPerformanceDTO.builder()
                                .staffId(staffId)
                                .staffName(staffName)
                                .staffRole("Nhân viên bán hàng")
                                .totalOrders(totalOrders)
                                .totalRevenue(totalRevenue)
                                .averageOrderValue(avgOrderValue)
                                .totalCustomers(0L)
                                .build());
                    }
                }
            }

        } catch (Exception e) {
            log.error("❌ Error getting top staff: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Lấy báo cáo chi tiết theo khoảng thời gian
     */
    public RevenueStatisticsDTO getRevenueStatistics(LocalDate fromDate, LocalDate toDate) {
        log.info("📊 Getting revenue statistics from {} to {}", fromDate, toDate);

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);

        try {
            // Doanh thu theo khoảng thời gian
            BigDecimal onlineRevenue = getSafeValue(() -> statisticsRepository.getOnlineRevenue(start, end));
            Long onlineOrders = getSafeLong(() -> statisticsRepository.getOnlineOrderCount(start, end));
            Long onlineCustomers = getSafeLong(() -> statisticsRepository.getOnlineCustomerCount(start, end));

            BigDecimal posRevenue = getSafeValue(() -> statisticsRepository.getPosRevenue(start, end));
            Long posOrders = getSafeLong(() -> statisticsRepository.getPosOrderCount(start, end));
            Long posCustomers = getSafeLong(() -> statisticsRepository.getPosCustomerCount(start, end));

            BigDecimal totalRevenue = onlineRevenue.add(posRevenue);
            Long totalOrders = onlineOrders + posOrders;
            Long totalCustomers = onlineCustomers + posCustomers;

            BigDecimal avgOrderValue = totalOrders > 0
                    ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // ===== TỔNG DOANH THU TÍCH LŨY (ALL TIME) =====
            BigDecimal totalOnlineRevenueAllTime = getSafeValue(() -> statisticsRepository.getTotalOnlineRevenueAllTime());
            BigDecimal totalPosRevenueAllTime = getSafeValue(() -> statisticsRepository.getTotalPosRevenueAllTime());
            Long totalOnlineOrdersAllTime = getSafeLong(() -> statisticsRepository.getTotalOnlineOrdersAllTime());
            Long totalPosOrdersAllTime = getSafeLong(() -> statisticsRepository.getTotalPosOrdersAllTime());

            BigDecimal totalRevenueAllTime = totalOnlineRevenueAllTime.add(totalPosRevenueAllTime);
            Long totalOrdersAllTime = totalOnlineOrdersAllTime + totalPosOrdersAllTime;

            log.info("📊 Revenue Stats - Period: {} to {}, Total: {}, All Time: {}",
                    fromDate, toDate, totalRevenue, totalRevenueAllTime);

            return RevenueStatisticsDTO.builder()
                    .totalRevenue(totalRevenue)
                    .totalOrders(totalOrders)
                    .totalCustomers(totalCustomers)
                    .averageOrderValue(avgOrderValue)
                    .onlineRevenue(onlineRevenue)
                    .onlineOrders(onlineOrders)
                    .posRevenue(posRevenue)
                    .posOrders(posOrders)
                    .totalRevenueAllTime(totalRevenueAllTime)
                    .totalOrdersAllTime(totalOrdersAllTime)
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error getting revenue statistics: {}", e.getMessage(), e);
            return getEmptyRevenueStatistics(fromDate, toDate);
        }
    }

    // =========================================
    // HELPER METHODS
    // =========================================

    private BigDecimal getSafeValue(ValueSupplier<BigDecimal> supplier) {
        try {
            BigDecimal value = supplier.get();
            return value != null ? value : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("⚠️ Error getting value: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Long getSafeLong(ValueSupplier<Long> supplier) {
        try {
            Long value = supplier.get();
            return value != null ? value : 0L;
        } catch (Exception e) {
            log.warn("⚠️ Error getting long value: {}", e.getMessage());
            return 0L;
        }
    }

    private DashboardStatisticsDTO getEmptyDashboardStatistics() {
        return DashboardStatisticsDTO.builder()
                .todayRevenue(BigDecimal.ZERO)
                .todayOrders(0L)
                .thisWeekRevenue(BigDecimal.ZERO)
                .thisWeekOrders(0L)
                .thisMonthRevenue(BigDecimal.ZERO)
                .thisMonthOrders(0L)
                .revenueGrowth(BigDecimal.ZERO)
                .orderGrowth(BigDecimal.ZERO)
                .onlineRevenue(BigDecimal.ZERO)
                .onlineOrders(0L)
                .posRevenue(BigDecimal.ZERO)
                .posOrders(0L)
                .totalCustomers(0L)
                .totalRevenueAllTime(BigDecimal.ZERO)
                .onlineRevenueAllTime(BigDecimal.ZERO)
                .posRevenueAllTime(BigDecimal.ZERO)
                .totalOrdersAllTime(0L)
                .dailyRevenue(new ArrayList<>())
                .topProducts(new ArrayList<>())
                .topStaff(new ArrayList<>())
                .build();
    }

    private RevenueStatisticsDTO getEmptyRevenueStatistics(LocalDate fromDate, LocalDate toDate) {
        return RevenueStatisticsDTO.builder()
                .totalRevenue(BigDecimal.ZERO)
                .totalOrders(0L)
                .totalCustomers(0L)
                .averageOrderValue(BigDecimal.ZERO)
                .onlineRevenue(BigDecimal.ZERO)
                .onlineOrders(0L)
                .posRevenue(BigDecimal.ZERO)
                .posOrders(0L)
                .totalRevenueAllTime(BigDecimal.ZERO)
                .totalOrdersAllTime(0L)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();
    }

    @FunctionalInterface
    interface ValueSupplier<T> {
        T get() throws Exception;
    }
}