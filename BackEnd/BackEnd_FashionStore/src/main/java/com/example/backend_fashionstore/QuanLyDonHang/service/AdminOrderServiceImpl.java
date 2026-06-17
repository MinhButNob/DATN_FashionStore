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
            orders = orderRepository.filterOrdersWithKeywordNative(status, keyword, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            orders = orderRepository.searchOrdersNative(keyword, pageable);
        } else if (status != null && !status.isEmpty()) {
            orders = orderRepository.filterOrdersNative(status, pageable);
        } else {
            orders = orderRepository.findAllActiveOrders(pageable);
        }

        return orders.map(this::convertToAdminResponse);
    }

    @Override
    public AdminOrderResponseDTO getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
        return convertToAdminResponse(order);
    }

    @Override
    public AdminOrderResponseDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        String oldStatus = order.getOrderStatus();
        String newStatus = request.getOrderStatus();

        // ✅ Kiểm tra chuyển đổi trạng thái hợp lệ
        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new RuntimeException("Không thể chuyển trạng thái từ '" + oldStatus + "' sang '" + newStatus + "'");
        }

        // ✅ Cập nhật trạng thái
        order.setOrderStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        // ✅ Nếu đơn hàng hoàn thành, cập nhật payment status
        if ("COMPLETED".equals(newStatus)) {
            order.setPaymentStatus("PAID");
        }

        // ✅ Nếu đơn hàng bị hủy, hoàn lại số lượng sản phẩm
        if ("CANCELLED".equals(newStatus)) {
            List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(orderId);
            for (OrderDetail detail : details) {
                ProductDetail pd = detail.getProductDetail();
                pd.setQuantity(pd.getQuantity() + detail.getQuantity());
                log.info("✅ Restored {} units for product detail: {}", detail.getQuantity(), pd.getProductDetailId());
            }
        }

        // ✅ Nếu đơn hàng đang giao hoặc đã hoàn thành, cập nhật tồn kho
        if ("SHIPPING".equals(newStatus) || "COMPLETED".equals(newStatus)) {
            // Tồn kho đã được trừ khi tạo đơn, không cần xử lý thêm
            log.info("📦 Order {} status updated to {}, stock already deducted", orderId, newStatus);
        }

        orderRepository.save(order);

        // ✅ Ghi lịch sử
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus(newStatus);
        history.setNote("Cập nhật trạng thái từ '" + getStatusText(oldStatus) + "' thành '" + getStatusText(newStatus) + "'. " +
                (request.getNotes() != null ? request.getNotes() : ""));
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Updated order {} status from {} to {}", orderId, oldStatus, newStatus);

        // ✅ GỬI THÔNG BÁO CHO KHÁCH HÀNG
        try {
            sendOrderStatusNotification(order, oldStatus, newStatus, request.getNotes());
        } catch (Exception e) {
            log.warn("⚠️ Không thể gửi thông báo cho khách hàng: {}", e.getMessage());
            // Vẫn tiếp tục cập nhật trạng thái dù không gửi được thông báo
        }

        return convertToAdminResponse(order);
    }

    /**
     * ✅ Gửi thông báo cho khách hàng khi đơn hàng thay đổi trạng thái
     */
    private void sendOrderStatusNotification(Order order, String oldStatus, String newStatus, String notes) {
        String customerName = order.getCustomer() != null ? order.getCustomer().getCustomerName() : "Khách hàng";
        String orderCode = "ORD" + String.format("%08d", order.getOrderId());

        String title = "Cập nhật đơn hàng #" + orderCode;
        String message = String.format(
                "Đơn hàng #%s của bạn đã được cập nhật trạng thái từ '%s' thành '%s'.",
                orderCode,
                getStatusText(oldStatus),
                getStatusText(newStatus)
        );

        if (notes != null && !notes.isEmpty()) {
            message += " Ghi chú: " + notes;
        }

        log.info("📧 SENDING NOTIFICATION:");
        log.info("   To: {} (ID: {})", customerName, order.getCustomer() != null ? order.getCustomer().getCustomerId() : "N/A");
        log.info("   Title: {}", title);
        log.info("   Message: {}", message);
        log.info("   Order: {} ({})", orderCode, order.getOrderId());
        log.info("   Status: {} -> {}", oldStatus, newStatus);

        // ✅ Có thể gửi notification qua WebSocket ở đây
        // webSocketService.sendNotificationToUser(customerId, notification);

        // ✅ Hoặc lưu vào database
        // notificationRepository.save(notification);

        // ✅ Hoặc gửi email
        // emailService.sendOrderStatusEmail(order, oldStatus, newStatus);
    }

    /**
     * ✅ Kiểm tra chuyển đổi trạng thái hợp lệ
     */
    private boolean isValidStatusTransition(String oldStatus, String newStatus) {
        // Không cho phép chuyển từ CANCELLED hoặc COMPLETED sang trạng thái khác
        if (("CANCELLED".equals(oldStatus) || "COMPLETED".equals(oldStatus)) && !oldStatus.equals(newStatus)) {
            log.warn("⚠️ Invalid status transition: {} -> {}", oldStatus, newStatus);
            return false;
        }
        return true;
    }

    /**
     * ✅ Lấy text hiển thị của trạng thái
     */
    private String getStatusText(String status) {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("PENDING_PAYMENT", "Chờ thanh toán");
        statusMap.put("PROCESSING", "Đang xử lý");
        statusMap.put("SHIPPING", "Đang giao hàng");
        statusMap.put("COMPLETED", "Hoàn thành");
        statusMap.put("CANCELLED", "Đã hủy");
        statusMap.put("RETURNED", "Trả hàng");
        return statusMap.getOrDefault(status, status);
    }

    @Override
    public AdminOrderResponseDTO updatePaymentStatus(Long orderId, String paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        String oldPaymentStatus = order.getPaymentStatus();
        order.setPaymentStatus(paymentStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Ghi lịch sử
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setNote("Cập nhật trạng thái thanh toán từ '" + oldPaymentStatus + "' thành '" + paymentStatus + "'");
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Updated order {} payment status from {} to {}", orderId, oldPaymentStatus, paymentStatus);

        return convertToAdminResponse(order);
    }

    @Override
    public OrderStatisticsDTO getStatistics() {
        long totalOrders = orderRepository.count();
        long completedOrders = orderRepository.countByOrderStatus("COMPLETED");
        long pendingOrders = orderRepository.countByOrderStatus("PENDING_PAYMENT");
        long cancelledOrders = orderRepository.countByOrderStatus("CANCELLED");
        long processingOrders = orderRepository.countByOrderStatus("PROCESSING");
        long shippingOrders = orderRepository.countByOrderStatus("SHIPPING");

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
                .processingOrders(processingOrders)
                .shippingOrders(shippingOrders)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .build();
    }

    @Override
    public Map<String, Long> getOrderStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("PENDING_PAYMENT", orderRepository.countByOrderStatus("PENDING_PAYMENT"));
        counts.put("PROCESSING", orderRepository.countByOrderStatus("PROCESSING"));
        counts.put("SHIPPING", orderRepository.countByOrderStatus("SHIPPING"));
        counts.put("COMPLETED", orderRepository.countByOrderStatus("COMPLETED"));
        counts.put("CANCELLED", orderRepository.countByOrderStatus("CANCELLED"));
        counts.put("RETURNED", orderRepository.countByOrderStatus("RETURNED"));
        return counts;
    }

    /**
     * ✅ Chuyển đổi Order entity sang AdminOrderResponseDTO
     */
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

        // Xử lý Shipping Address
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

        // Tính total amount
        BigDecimal subtotal = order.getOrderSubtotalAmount() != null ? order.getOrderSubtotalAmount() : BigDecimal.ZERO;
        BigDecimal discount = order.getOrderDiscountAmount() != null ? order.getOrderDiscountAmount() : BigDecimal.ZERO;
        BigDecimal shippingFee = order.getOrderShippingFee() != null ? order.getOrderShippingFee() : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.subtract(discount).add(shippingFee);

        // Xử lý Voucher
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
                .subtotalAmount(subtotal)
                .discountAmount(discount)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .customer(AdminCustomerInfoDTO.builder()
                        .customerId(order.getCustomer().getCustomerId())
                        .customerName(order.getCustomer().getCustomerName())
                        .phone(order.getCustomer().getPhone())
                        .email(order.getCustomer().getUser() != null ? order.getCustomer().getUser().getEmail() : null)
                        .build())
                .shippingAddress(shippingDTO)
                .voucher(voucherDTO)
                .items(itemResponses)
                .build();
    }
}