package com.example.backend_fashionstore.QuanLyDonHang.service;

import com.example.backend_fashionstore.QuanLyDonHang.dto.OrderStatisticsDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.AdminOrderResponseDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.UpdateOrderStatusRequest;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.AdminCustomerInfoDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.AdminShippingAddressDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.AdminVoucherInfoDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.AdminOrderItemResponseDTO;
import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderDetailRepository;
import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderRepository;
import com.example.backend_fashionstore.customer.order.repository.OrderHistoryRepository;
import com.example.backend_fashionstore.entity.Order;
import com.example.backend_fashionstore.entity.OrderDetail;
import com.example.backend_fashionstore.entity.OrderHistory;
import com.example.backend_fashionstore.entity.ProductDetail;
import com.example.backend_fashionstore.entity.Product;
import com.example.backend_fashionstore.entity.ShippingAddress;
import com.example.backend_fashionstore.entity.Voucher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderHistoryRepository orderHistoryRepository;

    @Override
    public Page<AdminOrderResponseDTO> getAllOrders(String status, LocalDateTime fromDate, LocalDateTime toDate, String keyword, Pageable pageable) {
        Page<Order> orders;

        log.info("getAllOrders - status: {}, fromDate: {}, toDate: {}, keyword: {}, pageable: {}",
                status, fromDate, toDate, keyword, pageable);

        // Xử lý theo từng trường hợp
        if (keyword != null && !keyword.isEmpty() && status != null && !status.isEmpty()) {
            // Có cả keyword và status
            orders = orderRepository.filterOrdersWithKeywordNative(status, keyword, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            // Chỉ có keyword
            orders = orderRepository.searchOrdersNative(keyword, pageable);
        } else if (status != null && !status.isEmpty()) {
            // Chỉ có status
            orders = orderRepository.filterOrdersNative(status, pageable);
        } else {
            // Không có gì, lấy tất cả
            orders = orderRepository.findAllActiveOrders(pageable);
        }

        return orders.map(this::convertToAdminResponse);
    }

    @Override
    public AdminOrderResponseDTO getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        return convertToAdminResponse(order);
    }

    @Override
    public AdminOrderResponseDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus(request.getOrderStatus());
        order.setUpdatedAt(LocalDateTime.now());

        orderRepository.save(order);

        // Ghi lịch sử
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setNote("Cập nhật trạng thái từ '" + oldStatus + "' thành '" + request.getOrderStatus() + "'. " + (request.getNotes() != null ? request.getNotes() : ""));
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        return convertToAdminResponse(order);
    }

    @Override
    public AdminOrderResponseDTO updatePaymentStatus(Long orderId, String paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        log.info("Cập nhật paymentStatus cho order {} thành: {} (Tính năng đang phát triển)", orderId, paymentStatus);

        // Ghi lịch sử
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setNote("Yêu cầu cập nhật trạng thái thanh toán thành: " + paymentStatus);
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        return convertToAdminResponse(order);
    }

    @Override
    public OrderStatisticsDTO getStatistics() {
        long totalOrders = orderRepository.count();
        long completedOrders = orderRepository.countByOrderStatus("COMPLETED");
        long pendingOrders = orderRepository.countByOrderStatus("PENDING_PAYMENT");
        long cancelledOrders = orderRepository.countByOrderStatus("CANCELLED");

        BigDecimal todayRevenue = orderRepository.getTodayRevenue();
        BigDecimal monthlyRevenue = orderRepository.getMonthlyRevenue(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue()
        );

        return OrderStatisticsDTO.builder()
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .pendingOrders(pendingOrders)
                .cancelledOrders(cancelledOrders)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .build();
    }

    @Override
    public Map<String, Long> getOrderStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("PENDING_PAYMENT", orderRepository.countByOrderStatus("PENDING_PAYMENT"));
        counts.put("PROCESSING", orderRepository.countByOrderStatus("PROCESSING"));
        counts.put("COMPLETED", orderRepository.countByOrderStatus("COMPLETED"));
        counts.put("CANCELLED", orderRepository.countByOrderStatus("CANCELLED"));
        counts.put("RETURNED", orderRepository.countByOrderStatus("RETURNED"));
        return counts;
    }

    private AdminOrderResponseDTO convertToAdminResponse(Order order) {
        List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(order.getOrderId());

        List<AdminOrderItemResponseDTO> itemResponses = details.stream()
                .map(detail -> {
                    ProductDetail pd = detail.getProductDetail();
                    Product product = pd.getProduct();

                    String imageUrl = null;
                    if (pd.getProductImages() != null && !pd.getProductImages().isEmpty()) {
                        imageUrl = pd.getProductImages().get(0).getProductImagePath();
                    }

                    return AdminOrderItemResponseDTO.builder()
                            .itemId(detail.getOrderDetailId())
                            .productDetailId(pd.getProductDetailId())
                            .productName(product.getProductName())
                            .colorName(pd.getColor() != null ? pd.getColor().getColorName() : null)
                            .sizeName(pd.getSize() != null ? pd.getSize().getSizeName() : null)
                            .imageUrl(imageUrl)
                            .quantity(detail.getQuantity())
                            .unitPrice(detail.getUnitPrice())
                            .discount(detail.getDiscount())
                            .finalAmount(detail.getFinalAmount())
                            .build();
                })
                .collect(Collectors.toList());

        ShippingAddress addr = order.getShippingAddress();
        AdminShippingAddressDTO shippingDTO = null;
        if (addr != null) {
            shippingDTO = AdminShippingAddressDTO.builder()
                    .addressId(addr.getShippingAddressId().intValue())
                    .fullName(addr.getShippingAddressFullName())
                    .phone(addr.getShippingAddressPhone())
                    .addressLine(addr.getShippingAddressAddressLine())
                    .city(addr.getShippingAddressCity())
                    .district(addr.getShippingAddressDistrict())
                    .ward(addr.getShippingAddressWard())
                    .build();
        }

        BigDecimal totalAmount = order.getOrderSubtotalAmount()
                .subtract(order.getOrderDiscountAmount() != null ? order.getOrderDiscountAmount() : BigDecimal.ZERO)
                .add(order.getOrderShippingFee() != null ? order.getOrderShippingFee() : BigDecimal.ZERO);

        Voucher voucher = order.getVoucher();
        AdminVoucherInfoDTO voucherDTO = null;
        if (voucher != null) {
            voucherDTO = AdminVoucherInfoDTO.builder()
                    .voucherId(voucher.getVoucherId())
                    .voucherCode(voucher.getVoucherCode())
                    .discountAmount(voucher.getVoucherDiscountAmount())
                    .discountPercent(voucher.getVoucherDiscountPercent())
                    .build();
        }

        return AdminOrderResponseDTO.builder()
                .orderId(order.getOrderId())
                .orderCode("ORD" + String.format("%08d", order.getOrderId()))
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus() : "UNPAID")
                .subtotalAmount(order.getOrderSubtotalAmount())
                .discountAmount(order.getOrderDiscountAmount())
                .shippingFee(order.getOrderShippingFee())
                .totalAmount(totalAmount)
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .customer(AdminCustomerInfoDTO.builder()
                        .customerId(order.getCustomer().getCustomerId())
                        .customerName(order.getCustomer().getCustomerName())
                        .phone(order.getCustomer().getPhone())
                        .email(order.getCustomer().getUser().getEmail())
                        .build())
                .shippingAddress(shippingDTO)
                .voucher(voucherDTO)
                .items(itemResponses)
                .build();
    }
}