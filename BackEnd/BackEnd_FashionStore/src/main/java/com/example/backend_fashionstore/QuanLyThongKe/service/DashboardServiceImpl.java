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
import java.time.LocalDateTime;
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
    private final KhachHangCustomerRepository customerRepository;

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

        if (currentMonth == null) currentMonth = BigDecimal.ZERO;
        if (previousMonth == null || previousMonth.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentMonth.subtract(previousMonth)
                .divide(previousMonth, 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
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