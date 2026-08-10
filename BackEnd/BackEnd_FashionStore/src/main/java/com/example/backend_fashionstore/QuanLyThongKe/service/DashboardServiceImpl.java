// DashboardServiceImpl.java
package com.example.backend_fashionstore.QuanLyThongKe.service;

import com.example.backend_fashionstore.QuanLyDonHang.dto.OrderStatisticsDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.AdminOrderResponseDTO;
import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderDetailRepository;
import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderRepository;
import com.example.backend_fashionstore.QuanLyDonHang.service.AdminOrderService;
import com.example.backend_fashionstore.QuanLySanPham.repository.Product.ProductRepository;
import com.example.backend_fashionstore.QuanLyThongKe.dto.DashboardResponseDTO;
import com.example.backend_fashionstore.QuanLyThongKe.dto.TopProductDTO;
import com.example.backend_fashionstore.entity.Order;
import com.example.backend_fashionstore.enums.UserStatus;
import com.example.backend_fashionstore.quanLyKhachHang.repository.KhachHangCustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final AdminOrderService adminOrderService;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository productDetailRepository;
    private final KhachHangCustomerRepository customerRepository;
    private final com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository.PosOrderRepository posOrderRepository;

    @Override
    public DashboardResponseDTO getDashboardData() {
        log.info("📊 Building dashboard data");

        // 1. Thống kê đơn hàng
        OrderStatisticsDTO orderStats = adminOrderService.getStatistics();

        // 2. Thống kê sản phẩm
        DashboardResponseDTO.ProductStatisticsDTO productStats = getProductStatistics();

        // 3. Thống kê khách hàng
        DashboardResponseDTO.CustomerStatisticsDTO customerStats = getCustomerStatistics();

        // 4. Doanh thu theo tháng (năm hiện tại)
        int currentYear = LocalDateTime.now().getYear();
        Map<Integer, BigDecimal> monthlyRevenue = getMonthlyRevenue(currentYear);

        // 5. Top sản phẩm bán chạy
        List<TopProductDTO> topProducts = getTopProducts(5);

        // 6. Đơn hàng gần đây
        Pageable top5 = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        List<AdminOrderResponseDTO> recentOrders = orderRepository.findTop5ByOrderByCreatedAtDesc(top5)
                .stream()
                .map(this::convertToAdminResponse)
                .collect(Collectors.toList());

        // 7. Tăng trưởng doanh thu
        BigDecimal revenueGrowth = getRevenueGrowth();

        return DashboardResponseDTO.builder()
                .orderStatistics(orderStats)
                .productStatistics(productStats)
                .customerStatistics(customerStats)
                .monthlyRevenue(monthlyRevenue)
                .topProducts(topProducts)
                .recentOrders(recentOrders)
                .revenueGrowth(revenueGrowth)
                .build();
    }

    @Override
    public Map<Integer, BigDecimal> getMonthlyRevenue(int year) {
        log.info("📊 Getting monthly revenue for year: {}", year);

        List<Object[]> results = orderRepository.getMonthlyRevenueByYear(year);
        Map<Integer, BigDecimal> monthlyRevenue = new LinkedHashMap<>();

        // Khởi tạo tất cả 12 tháng với giá trị 0
        for (int month = 1; month <= 12; month++) {
            monthlyRevenue.put(month, BigDecimal.ZERO);
        }

        for (Object[] row : results) {
            Integer month = ((Number) row[0]).intValue();
            BigDecimal revenue = (BigDecimal) row[1];
            monthlyRevenue.put(month, revenue != null ? revenue : BigDecimal.ZERO);
        }

        // Thêm doanh thu POS
        List<Object[]> posResults = posOrderRepository.getMonthlyPosRevenueByYear(year);
        for (Object[] row : posResults) {
            Integer month = ((Number) row[0]).intValue();
            BigDecimal posRevenue = (BigDecimal) row[1];
            if (posRevenue != null && posRevenue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal current = monthlyRevenue.get(month);
                monthlyRevenue.put(month, current.add(posRevenue));
            }
        }

        return monthlyRevenue;
    }

    @Override
    public List<TopProductDTO> getTopProducts(int limit) {
        log.info("🏆 Getting top {} products", limit);

        List<Object[]> results = orderDetailRepository.getTopSellingProductsWithDetails(limit);
        List<TopProductDTO> topProducts = new ArrayList<>();

        for (Object[] row : results) {
            TopProductDTO dto = TopProductDTO.builder()
                    .productId(((Number) row[0]).intValue())
                    .productName((String) row[1])
                    .totalSold(((Number) row[2]).longValue())
                    .revenue((BigDecimal) row[3])
                    .imageUrl(row.length > 4 ? (String) row[4] : null)
                    .build();
            topProducts.add(dto);
        }

        return topProducts;
    }

    @Override
    public BigDecimal getRevenueGrowth() {
        BigDecimal currentMonth = orderRepository.getCurrentMonthRevenue();
        BigDecimal previousMonth = orderRepository.getPreviousMonthRevenue();
        
        BigDecimal currentPosMonth = posOrderRepository.getCurrentMonthPosRevenue();
        BigDecimal previousPosMonth = posOrderRepository.getPreviousMonthPosRevenue();

        if (currentMonth == null) currentMonth = BigDecimal.ZERO;
        if (currentPosMonth != null) currentMonth = currentMonth.add(currentPosMonth);
        
        if (previousMonth == null) previousMonth = BigDecimal.ZERO;
        if (previousPosMonth != null) previousMonth = previousMonth.add(previousPosMonth);
        if (previousMonth == null || previousMonth.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentMonth.subtract(previousMonth)
                .divide(previousMonth, 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    @Override
    public Map<Integer, BigDecimal> getDailyRevenue(int year, int month) {
        log.info("📊 Getting daily revenue for year: {}, month: {}", year, month);
        List<Object[]> results = orderRepository.getDailyRevenueByMonth(year, month);
        Map<Integer, BigDecimal> dailyRevenue = new LinkedHashMap<>();

        // Khởi tạo các ngày trong tháng (vd: 31 ngày)
        int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            dailyRevenue.put(day, BigDecimal.ZERO);
        }

        for (Object[] row : results) {
            Integer day = ((Number) row[0]).intValue();
            BigDecimal revenue = (BigDecimal) row[1];
            dailyRevenue.put(day, revenue != null ? revenue : BigDecimal.ZERO);
        }
        
        // Thêm doanh thu POS
        List<Object[]> posResults = posOrderRepository.getDailyPosRevenueByMonth(year, month);
        for (Object[] row : posResults) {
            Integer day = ((Number) row[0]).intValue();
            BigDecimal posRevenue = (BigDecimal) row[1];
            if (posRevenue != null && posRevenue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal current = dailyRevenue.get(day);
                dailyRevenue.put(day, current.add(posRevenue));
            }
        }
        
        return dailyRevenue;
    }

    @Override
    public Map<String, Long> getOrderStatusStats(String filterType, String dateRange, String month, Integer year) {
        log.info("📊 Getting order status stats. type={}, range={}, month={}, year={}", filterType, dateRange, month, year);
        LocalDateTime startDate;
        LocalDateTime endDate;

        if ("Ngày".equals(filterType) && dateRange != null && !dateRange.isEmpty()) {
            // Định dạng: MM/DD/YYYY - MM/DD/YYYY hoặc DD/MM/YYYY
            String[] dates = dateRange.split(" - ");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy"); // assuming frontend passes dd/MM/yyyy
            try {
                startDate = LocalDate.parse(dates[0], formatter).atStartOfDay();
                endDate = LocalDate.parse(dates[1], formatter).atTime(LocalTime.MAX);
            } catch (Exception e) {
                log.warn("Lỗi parse date: " + dateRange + ". Fallback to current month.");
                startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
                endDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
            }
        } else if ("Tháng".equals(filterType) && month != null && !month.isEmpty()) {
            // Định dạng: MM/YYYY
            String[] parts = month.split("/");
            int m = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            startDate = LocalDate.of(y, m, 1).atStartOfDay();
            endDate = LocalDate.of(y, m, 1).with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
        } else if ("Năm".equals(filterType) && year != null) {
            startDate = LocalDate.of(year, 1, 1).atStartOfDay();
            endDate = LocalDate.of(year, 12, 31).atTime(LocalTime.MAX);
        } else {
            startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
            endDate = LocalDate.now().with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX);
        }

        List<Object[]> results = orderRepository.countOrderStatusByDateRange(startDate, endDate);
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Object[] row : results) {
            String status = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            stats.put(status, count);
        }
        return stats;
    }

    @Override
    public List<com.example.backend_fashionstore.QuanLyThongKe.dto.InventoryAlertDTO> getInventoryAlerts(int threshold, int limit) {
        log.info("⚠️ Getting inventory alerts threshold < {}", threshold);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        List<com.example.backend_fashionstore.entity.ProductDetail> details = productDetailRepository.findLowStockVariants(threshold, pageable);
        
        return details.stream().map(pd -> {
            String imgUrl = null;
            if (pd.getProductImages() != null && !pd.getProductImages().isEmpty()) {
                imgUrl = pd.getProductImages().get(0).getProductImagePath();
            }
            return com.example.backend_fashionstore.QuanLyThongKe.dto.InventoryAlertDTO.builder()
                    .productDetailId(pd.getProductDetailId())
                    .productName(pd.getProduct().getProductName())
                    .sizeName(pd.getSize() != null ? pd.getSize().getSizeName() : "")
                    .colorName(pd.getColor() != null ? pd.getColor().getColorName() : "")
                    .quantity(pd.getQuantity())
                    .imageUrl(imgUrl)
                    .build();
        }).collect(Collectors.toList());
    }

    // ===== HELPER METHODS =====

    private DashboardResponseDTO.ProductStatisticsDTO getProductStatistics() {
        try {
            long totalProducts = productRepository.countActiveProducts();
            long inStockVariants = productRepository.countInStockVariants();
            long lowStockVariants = productRepository.countLowStockVariants(10);

            long deletedProducts = productRepository.countByDeletedAtIsNotNull();

            return DashboardResponseDTO.ProductStatisticsDTO.builder()
                    .totalProducts(totalProducts)
                    .inStockVariants(inStockVariants)
                    .lowStockVariants(lowStockVariants)
                    .deletedProducts(deletedProducts)
                    .build();
        } catch (Exception e) {
            log.warn("Cannot get product statistics: {}", e.getMessage());
            return DashboardResponseDTO.ProductStatisticsDTO.builder()
                    .totalProducts(0L)
                    .inStockVariants(0L)
                    .lowStockVariants(0L)
                    .deletedProducts(0L)
                    .build();
        }
    }

    private DashboardResponseDTO.CustomerStatisticsDTO getCustomerStatistics() {
        try {
            long total = customerRepository.countAllCustomers();
            long active = customerRepository.countByUserStatus(UserStatus.ACTIVE);
            long locked = customerRepository.countByUserStatus(UserStatus.BANNED);
            long deleted = customerRepository.countByUserStatus(UserStatus.DELETED);

            return DashboardResponseDTO.CustomerStatisticsDTO.builder()
                    .totalCustomers(total)
                    .activeCustomers(active)
                    .lockedCustomers(locked)
                    .deletedCustomers(deleted)
                    .build();
        } catch (Exception e) {
            log.warn("Cannot get customer statistics: {}", e.getMessage());
            return DashboardResponseDTO.CustomerStatisticsDTO.builder()
                    .totalCustomers(0L)
                    .activeCustomers(0L)
                    .lockedCustomers(0L)
                    .deletedCustomers(0L)
                    .build();
        }
    }

    // ===== CONVERT TO ADMIN RESPONSE =====
    private AdminOrderResponseDTO convertToAdminResponse(Order order) {
        // Sử dụng phương thức convertToAdminResponse từ AdminOrderServiceImpl
        // Hoặc implement đơn giản ở đây
        return AdminOrderResponseDTO.builder()
                .orderId(order.getOrderId())
                .orderCode("ORD" + String.format("%08d", order.getOrderId()))
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus().toString())
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().toString() : "UNPAID")
                .totalAmount(order.getOrderTotalAmount())
                .createdAt(order.getCreatedAt())
                .customer(com.example.backend_fashionstore.QuanLyDonHang.dto.response.AdminCustomerInfoDTO.builder()
                        .customerId(order.getCustomer().getCustomerId())
                        .customerName(order.getCustomer().getCustomerName())
                        .phone(order.getCustomer().getPhone())
                        .email(order.getCustomer().getUser() != null ? order.getCustomer().getUser().getEmail() : null)
                        .build())
                .build();
    }
}