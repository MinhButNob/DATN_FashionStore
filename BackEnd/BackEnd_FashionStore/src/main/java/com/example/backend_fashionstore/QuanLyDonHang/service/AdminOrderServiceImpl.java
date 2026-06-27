package com.example.backend_fashionstore.QuanLyDonHang.service;

import com.example.backend_fashionstore.QuanLyDonHang.dto.OrderStatisticsDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.*;
import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderDetailRepository;
import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderRepository;
import com.example.backend_fashionstore.customer.order.repository.OrderHistoryRepository;
import com.example.backend_fashionstore.customer.order.repository.ReturnRequestRepository;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.enums.OrderStatus;
import com.example.backend_fashionstore.enums.PaymentStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
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
    private final ReturnRequestRepository returnRequestRepository;

    // ================================================================
    // ===== ĐƠN HÀNG =====
    // ================================================================

    @Override
    public Page<AdminOrderResponseDTO> getAllOrders(String status, LocalDateTime fromDate, LocalDateTime toDate, String keyword, Pageable pageable) {
        Page<Order> orders;

        log.info("getAllOrders - status: {}, fromDate: {}, toDate: {}, keyword: {}, pageable: {}",
                status, fromDate, toDate, keyword, pageable);

        // 🆕 Xử lý trường hợp filter REJECTED
        if ("REJECTED".equals(status)) {
            // Lấy danh sách order_id từ return_requests có status = REJECTED
            List<Long> orderIds = returnRequestRepository.findOrderIdsByStatus("REJECTED");
            if (orderIds.isEmpty()) {
                return Page.empty(pageable);
            }
            orders = orderRepository.findByOrderIdIn(orderIds, pageable);
            return orders.map(this::convertToAdminResponse);
        }

        OrderStatus orderStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                orderStatus = OrderStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid order status: {}", status);
            }
        }

        if (keyword != null && !keyword.isEmpty() && orderStatus != null) {
            orders = orderRepository.filterOrdersWithKeyword(orderStatus, keyword, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            orders = orderRepository.searchOrdersNative(keyword, pageable);
        } else if (orderStatus != null) {
            orders = orderRepository.filterOrders(orderStatus, pageable);
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

        String oldStatus = order.getOrderStatus().toString();
        String newStatus = request.getOrderStatus();

        // Kiểm tra chuyển trạng thái hợp lệ
        if (!isValidAdminStatusTransition(oldStatus, newStatus)) {
            throw new RuntimeException("Không thể chuyển trạng thái từ '" + oldStatus + "' sang '" + newStatus + "'");
        }

        // Cập nhật trạng thái
        order.setOrderStatus(OrderStatus.valueOf(newStatus));
        order.setUpdatedAt(LocalDateTime.now());

        // Nếu chuyển từ RETURN_RECEIVED sang RETURNED, cập nhật trạng thái return request
        if ("RETURN_RECEIVED".equals(oldStatus) && "RETURNED".equals(newStatus)) {
            // Cập nhật trạng thái return request thành COMPLETED
            returnRequestRepository.findFirstByOrderIdAndDeletedAtIsNull(orderId)
                    .ifPresent(returnRequest -> {
                        returnRequest.setStatus("COMPLETED");
                        returnRequest.setUpdatedAt(LocalDateTime.now());
                        returnRequestRepository.save(returnRequest);
                        log.info("✅ Return request completed for order {}", orderId);
                    });
        }

        // Nếu đơn hàng bị hủy, hoàn lại số lượng sản phẩm
        if ("CANCELLED".equals(newStatus)) {
            List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(orderId);
            for (OrderDetail detail : details) {
                ProductDetail pd = detail.getProductDetail();
                pd.setQuantity(pd.getQuantity() + detail.getQuantity());
                log.info("✅ Restored {} units for product detail: {}", detail.getQuantity(), pd.getProductDetailId());
            }
        }

        orderRepository.save(order);

        // Ghi lịch sử
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus(newStatus);
        history.setNote("Admin cập nhật trạng thái từ '" + getStatusText(oldStatus) + "' thành '" + getStatusText(newStatus) + "'. " +
                (request.getNotes() != null ? request.getNotes() : ""));
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Admin updated order {} status from {} to {}", orderId, oldStatus, newStatus);

        try {
            sendOrderStatusNotification(order, oldStatus, newStatus, request.getNotes());
        } catch (Exception e) {
            log.warn("⚠️ Không thể gửi thông báo cho khách hàng: {}", e.getMessage());
        }

        return convertToAdminResponse(order);
    }

    @Override
    public AdminOrderResponseDTO updatePaymentStatus(Long orderId, String paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        String oldPaymentStatus = order.getPaymentStatus().toString();
        order.setPaymentStatus(PaymentStatus.valueOf(paymentStatus));
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

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

        long completedOrders = orderRepository.countByOrderStatus(OrderStatus.COMPLETED);
        long pendingOrders = orderRepository.countByOrderStatus(OrderStatus.PENDING_PAYMENT);
        long cancelledOrders = orderRepository.countByOrderStatus(OrderStatus.CANCELLED);
        long processingOrders = orderRepository.countByOrderStatus(OrderStatus.PROCESSING);
        long shippingOrders = orderRepository.countByOrderStatus(OrderStatus.SHIPPING);
        long confirmedOrders = orderRepository.countByOrderStatus(OrderStatus.CONFIRMED);
        long deliveredOrders = orderRepository.countByOrderStatus(OrderStatus.DELIVERED);
        long returnedOrders = orderRepository.countByOrderStatus(OrderStatus.RETURNED);
        long rejectedOrders = orderRepository.countByOrderStatus(OrderStatus.REJECTED);

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
                .confirmedOrders(confirmedOrders)
                .deliveredOrders(deliveredOrders)
                .returnedOrders(returnedOrders)
                .rejectedOrders(rejectedOrders)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .build();
    }

    @Override
    public Map<String, Long> getOrderStatusCounts() {
        Map<String, Long> counts = new HashMap<>();

        counts.put("PENDING_PAYMENT", orderRepository.countByOrderStatus(OrderStatus.PENDING_PAYMENT));
        counts.put("CONFIRMED", orderRepository.countByOrderStatus(OrderStatus.CONFIRMED));
        counts.put("PROCESSING", orderRepository.countByOrderStatus(OrderStatus.PROCESSING));
        counts.put("SHIPPING", orderRepository.countByOrderStatus(OrderStatus.SHIPPING));
        counts.put("DELIVERED", orderRepository.countByOrderStatus(OrderStatus.DELIVERED));
        counts.put("COMPLETED", orderRepository.countByOrderStatus(OrderStatus.COMPLETED));
        counts.put("CANCELLED", orderRepository.countByOrderStatus(OrderStatus.CANCELLED));
        counts.put("RETURN_REQUESTED", orderRepository.countByOrderStatus(OrderStatus.RETURN_REQUESTED));
        counts.put("RETURN_APPROVED", orderRepository.countByOrderStatus(OrderStatus.RETURN_APPROVED));
        counts.put("RETURN_RECEIVED", orderRepository.countByOrderStatus(OrderStatus.RETURN_RECEIVED));
        counts.put("RETURN_PENDING", orderRepository.countByOrderStatus(OrderStatus.RETURN_PENDING));
        counts.put("RETURNED", orderRepository.countByOrderStatus(OrderStatus.RETURNED));
        counts.put("REJECTED", orderRepository.countByOrderStatus(OrderStatus.REJECTED));

        return counts;
    }

    // ================================================================
    // ===== 🆕 QUẢN LÝ HOÀN HÀNG =====
    // ================================================================

    @Override
    public ReturnRequestDetailDTO getReturnRequestByOrderId(Long orderId) {
        log.info("📋 Getting return request for order: {}", orderId);

        // Tìm yêu cầu hoàn hàng
        ReturnRequest returnRequest = returnRequestRepository
                .findFirstByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn hàng cho đơn hàng này"));

        // Lấy thông tin đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Parse images từ JSON
        List<String> images = null;
        if (returnRequest.getImages() != null && !returnRequest.getImages().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                images = objectMapper.readValue(
                        returnRequest.getImages(),
                        new TypeReference<List<String>>() {}
                );
            } catch (Exception e) {
                log.warn("Cannot parse images: {}", e.getMessage());
            }
        }

        // Tạo DTO
        return ReturnRequestDetailDTO.builder()
                .orderId(returnRequest.getOrderId())
                .orderCode("ORD" + String.format("%08d", order.getOrderId()))
                .reason(returnRequest.getReason())
                .description(returnRequest.getDescription())
                .images(images)
                .status(returnRequest.getStatus())
                .createdAt(returnRequest.getCreatedAt())
                .updatedAt(returnRequest.getUpdatedAt())
                .adminNote(returnRequest.getAdminNote())
                .customerName(order.getCustomer() != null ? order.getCustomer().getCustomerName() : null)
                .customerPhone(order.getCustomer() != null ? order.getCustomer().getPhone() : null)
                .customerEmail(order.getCustomer() != null && order.getCustomer().getUser() != null ?
                        order.getCustomer().getUser().getEmail() : null)
                .build();
    }

    @Override
    @Transactional
    public ReturnRequestDetailDTO processReturnRequest(Long orderId, String status, String adminNote) {
        log.info("🔄 Processing return request for order: {}, status: {}", orderId, status);

        // 1. Kiểm tra yêu cầu tồn tại
        ReturnRequest returnRequest = returnRequestRepository
                .findFirstByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn hàng"));

        // 2. Kiểm tra trạng thái hiện tại
        if (!"PENDING".equals(returnRequest.getStatus())) {
            throw new RuntimeException("Yêu cầu hoàn hàng đã được xử lý trước đó");
        }

        // 3. Cập nhật trạng thái return request
        returnRequest.setStatus(status);
        if (adminNote != null && !adminNote.isEmpty()) {
            String currentNote = returnRequest.getAdminNote();
            returnRequest.setAdminNote(currentNote != null ? currentNote + "\n" + adminNote : adminNote);
        }
        returnRequest.setUpdatedAt(LocalDateTime.now());
        returnRequestRepository.save(returnRequest);

        // 4. Cập nhật trạng thái đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if ("APPROVED".equals(status)) {
            order.setOrderStatus(OrderStatus.RETURN_APPROVED);
            log.info("✅ Return request APPROVED for order {}", orderId);
        } else if ("REJECTED".equals(status)) {
            // Khi từ chối, đặt trạng thái là REJECTED
            order.setOrderStatus(OrderStatus.REJECTED);
            log.info("❌ Return request REJECTED for order {}", orderId);
        } else {
            throw new RuntimeException("Trạng thái không hợp lệ. Chỉ chấp nhận APPROVED hoặc REJECTED");
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 5. Ghi lịch sử
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus(order.getOrderStatus().toString());
        String actionText = "APPROVED".equals(status) ? "duyệt" : "từ chối";
        String note = "Admin " + actionText + " yêu cầu hoàn hàng";
        if (adminNote != null && !adminNote.isEmpty()) {
            note += ". Phản hồi: " + adminNote;
        }
        history.setNote(note);
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Return request processed for order {}: {}", orderId, status);

        return getReturnRequestByOrderId(orderId);
    }

    @Override
    @Transactional
    public ReturnRequestDetailDTO receiveReturnedItems(Long orderId, String note) {
        log.info("📦 Admin received returned items for order: {}", orderId);

        // 1. Kiểm tra yêu cầu tồn tại
        ReturnRequest returnRequest = returnRequestRepository
                .findFirstByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn hàng"));

        // 2. Kiểm tra trạng thái hiện tại (chỉ cho phép từ APPROVED)
        if (!"APPROVED".equals(returnRequest.getStatus())) {
            throw new RuntimeException("Chỉ có thể xác nhận nhận hàng khi yêu cầu đã được duyệt");
        }

        // 3. Cập nhật trạng thái return request thành RECEIVED
        returnRequest.setStatus("RECEIVED");
        if (note != null && !note.isEmpty()) {
            String adminNote = returnRequest.getAdminNote() != null ?
                    returnRequest.getAdminNote() + "\n" + note : note;
            returnRequest.setAdminNote(adminNote);
        }
        returnRequest.setUpdatedAt(LocalDateTime.now());
        returnRequestRepository.save(returnRequest);

        // 4. Cập nhật trạng thái đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        order.setOrderStatus(OrderStatus.RETURN_RECEIVED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 5. Ghi lịch sử
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus("RETURN_RECEIVED");
        String historyNote = "Admin xác nhận đã nhận hàng trả";
        if (note != null && !note.isEmpty()) {
            historyNote += ". Ghi chú: " + note;
        }
        history.setNote(historyNote);
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Returned items received for order {}", orderId);

        return getReturnRequestByOrderId(orderId);
    }

    @Override
    public Page<ReturnRequestDetailDTO> getAllReturnRequests(String status, Pageable pageable) {
        log.info("📋 Getting all return requests, status: {}", status);

        Page<ReturnRequest> requests;
        if (status != null && !status.isEmpty()) {
            requests = returnRequestRepository.findByStatusAndDeletedAtIsNull(status, pageable);
        } else {
            requests = returnRequestRepository.findByDeletedAtIsNull(pageable);
        }

        return requests.map(r -> {
            Order order = orderRepository.findById(r.getOrderId()).orElse(null);
            return convertToReturnRequestDetailDTO(r, order);
        });
    }

    // ================================================================
    // ===== HELPER METHODS =====
    // ================================================================

    // Cập nhật phương thức isValidAdminStatusTransition
    private boolean isValidAdminStatusTransition(String oldStatus, String newStatus) {
        // Admin không thể chuyển sang CANCELLED (do khách hàng hủy)
        if ("CANCELLED".equals(newStatus)) {
            return false;
        }

        // Admin có thể chuyển từ RETURN_APPROVED -> RETURN_RECEIVED
        if ("RETURN_APPROVED".equals(oldStatus) && "RETURN_RECEIVED".equals(newStatus)) {
            return true;
        }

        // Admin có thể chuyển từ RETURN_RECEIVED -> RETURNED
        if ("RETURN_RECEIVED".equals(oldStatus) && "RETURNED".equals(newStatus)) {
            return true;
        }

        // Admin chỉ có thể chuyển theo thứ tự:
        // PENDING_PAYMENT -> CONFIRMED -> PROCESSING -> SHIPPING -> DELIVERED -> COMPLETED
        List<String> validSequence = Arrays.asList(
                "PENDING_PAYMENT", "CONFIRMED", "PROCESSING", "SHIPPING", "DELIVERED", "COMPLETED"
        );
        int oldIndex = validSequence.indexOf(oldStatus);
        int newIndex = validSequence.indexOf(newStatus);

        // Chỉ cho phép chuyển sang trạng thái tiếp theo trong chuỗi
        return newIndex == oldIndex + 1;
    }

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
    }

    private String getStatusText(String status) {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("PENDING_PAYMENT", "Chờ thanh toán");
        statusMap.put("CONFIRMED", "Đã xác nhận");
        statusMap.put("PROCESSING", "Đang xử lý");
        statusMap.put("SHIPPING", "Đang giao hàng");
        statusMap.put("DELIVERED", "Đã giao hàng");
        statusMap.put("COMPLETED", "Hoàn thành");
        statusMap.put("CANCELLED", "Đã hủy");
        statusMap.put("RETURN_REQUESTED", "Yêu cầu hoàn hàng");
        statusMap.put("RETURN_APPROVED", "Đã duyệt hoàn hàng");
        statusMap.put("RETURN_RECEIVED", "Đã nhận hàng trả");
        statusMap.put("RETURN_PENDING", "Đang thu hồi hàng");
        statusMap.put("RETURNED", "Đã hoàn hàng");
        statusMap.put("REJECTED", "Đã từ chối hoàn trả");
        return statusMap.getOrDefault(status, status);
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

        // ✅ LẤY SHIPPING ADDRESS TỪ ORDER
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
            log.info("📦 Shipping address found for order {}: {}", order.getOrderId(), shippingDTO);
        } else {
            // ✅ NẾU KHÔNG CÓ SHIPPING ADDRESS, LẤY THÔNG TIN TỪ CUSTOMER
            Customer customer = order.getCustomer();
            if (customer != null) {
                shippingDTO = AdminShippingAddressDTO.builder()
                        .addressId(null)
                        .fullName(customer.getCustomerName())
                        .phone(customer.getPhone())
                        .addressLine(customer.getAddress())
                        .city("")
                        .district("")
                        .ward("")
                        .build();
                log.info("📦 Using customer address for order {}: {}", order.getOrderId(), shippingDTO);
            }
        }

        BigDecimal subtotal = order.getOrderSubtotalAmount() != null ? order.getOrderSubtotalAmount() : BigDecimal.ZERO;
        BigDecimal discount = order.getOrderDiscountAmount() != null ? order.getOrderDiscountAmount() : BigDecimal.ZERO;
        BigDecimal shippingFee = order.getOrderShippingFee() != null ? order.getOrderShippingFee() : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.subtract(discount).add(shippingFee);

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
                .orderStatus(order.getOrderStatus().toString())
                .paymentStatus(order.getPaymentStatus() != null ?
                        order.getPaymentStatus().toString() : PaymentStatus.UNPAID.toString())
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
                .shippingAddress(shippingDTO) // ✅ ĐẢM BẢO GÁN SHIPPING ADDRESS
                .voucher(voucherDTO)
                .items(itemResponses)
                .build();
    }

    private ReturnRequestDetailDTO convertToReturnRequestDetailDTO(ReturnRequest returnRequest, Order order) {
        // Parse images từ JSON
        List<String> images = null;
        if (returnRequest.getImages() != null && !returnRequest.getImages().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                images = objectMapper.readValue(
                        returnRequest.getImages(),
                        new TypeReference<List<String>>() {}
                );
            } catch (Exception e) {
                log.warn("Cannot parse images: {}", e.getMessage());
            }
        }

        String orderCode = order != null ? "ORD" + String.format("%08d", order.getOrderId()) : null;

        return ReturnRequestDetailDTO.builder()
                .orderId(returnRequest.getOrderId())
                .orderCode(orderCode)
                .reason(returnRequest.getReason())
                .description(returnRequest.getDescription())
                .images(images)
                .status(returnRequest.getStatus())
                .createdAt(returnRequest.getCreatedAt())
                .updatedAt(returnRequest.getUpdatedAt())
                .adminNote(returnRequest.getAdminNote())
                .customerName(order != null && order.getCustomer() != null ?
                        order.getCustomer().getCustomerName() : null)
                .customerPhone(order != null && order.getCustomer() != null ?
                        order.getCustomer().getPhone() : null)
                .customerEmail(order != null && order.getCustomer() != null && order.getCustomer().getUser() != null ?
                        order.getCustomer().getUser().getEmail() : null)
                .build();
    }

    @Override
    public InvoiceDTO getInvoice(Long orderId) {
        log.info("📄 Generating invoice for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        // Lấy danh sách sản phẩm
        List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(orderId);

        List<InvoiceItemDTO> items = details.stream()
                .map(detail -> {
                    ProductDetail pd = detail.getProductDetail();
                    Product product = pd.getProduct();

                    String imageUrl = null;
                    if (pd.getProductImages() != null && !pd.getProductImages().isEmpty()) {
                        imageUrl = pd.getProductImages().get(0).getProductImagePath();
                    }

                    return InvoiceItemDTO.builder()
                            .productName(product.getProductName())
                            .colorName(pd.getColor() != null ? pd.getColor().getColorName() : null)
                            .sizeName(pd.getSize() != null ? pd.getSize().getSizeName() : null)
                            .imageUrl(imageUrl)
                            .quantity(detail.getQuantity())
                            .unitPrice(detail.getUnitPrice())
                            .finalAmount(detail.getFinalAmount())
                            .build();
                })
                .collect(Collectors.toList());

        // Thông tin địa chỉ giao hàng
        ShippingAddress addr = order.getShippingAddress();
        String shippingAddress = "";
        String shippingCity = "";
        String shippingDistrict = "";
        String shippingWard = "";
        String shippingFullName = "";
        String shippingPhone = "";

        if (addr != null) {
            shippingFullName = addr.getShippingAddressFullName() != null ? addr.getShippingAddressFullName() : "";
            shippingPhone = addr.getShippingAddressPhone() != null ? addr.getShippingAddressPhone() : "";
            shippingAddress = addr.getShippingAddressAddressLine() != null ? addr.getShippingAddressAddressLine() : "";
            shippingCity = addr.getShippingAddressCity() != null ? addr.getShippingAddressCity() : "";
            shippingDistrict = addr.getShippingAddressDistrict() != null ? addr.getShippingAddressDistrict() : "";
            shippingWard = addr.getShippingAddressWard() != null ? addr.getShippingAddressWard() : "";
        } else {
            // Nếu không có shipping address, lấy từ customer
            Customer customer = order.getCustomer();
            if (customer != null) {
                shippingFullName = customer.getCustomerName() != null ? customer.getCustomerName() : "";
                shippingPhone = customer.getPhone() != null ? customer.getPhone() : "";
                shippingAddress = customer.getAddress() != null ? customer.getAddress() : "";
            }
        }

        // Thông tin voucher
        Voucher voucher = order.getVoucher();
        String voucherCode = null;
        BigDecimal voucherDiscountAmount = null;
        if (voucher != null) {
            voucherCode = voucher.getVoucherCode();
            voucherDiscountAmount = voucher.getVoucherDiscountAmount();
        }

        // Tính tổng tiền
        BigDecimal subtotal = order.getOrderSubtotalAmount() != null ? order.getOrderSubtotalAmount() : BigDecimal.ZERO;
        BigDecimal discount = order.getOrderDiscountAmount() != null ? order.getOrderDiscountAmount() : BigDecimal.ZERO;
        BigDecimal shippingFee = order.getOrderShippingFee() != null ? order.getOrderShippingFee() : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.subtract(discount).add(shippingFee);

        return InvoiceDTO.builder()
                .orderId(order.getOrderId())
                .orderCode("ORD" + String.format("%08d", order.getOrderId()))
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus().toString())
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().toString() : "UNPAID")
                .subtotalAmount(subtotal)
                .discountAmount(discount)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .notes(order.getNotes())
                .customerName(order.getCustomer() != null ? order.getCustomer().getCustomerName() : "")
                .customerPhone(order.getCustomer() != null ? order.getCustomer().getPhone() : "")
                .customerEmail(order.getCustomer() != null && order.getCustomer().getUser() != null ?
                        order.getCustomer().getUser().getEmail() : "")
                .shippingFullName(shippingFullName)
                .shippingPhone(shippingPhone)
                .shippingAddress(shippingAddress)
                .shippingCity(shippingCity)
                .shippingDistrict(shippingDistrict)
                .shippingWard(shippingWard)
                .voucherCode(voucherCode)
                .voucherDiscountAmount(voucherDiscountAmount)
                .items(items)
                .build();
    }
}