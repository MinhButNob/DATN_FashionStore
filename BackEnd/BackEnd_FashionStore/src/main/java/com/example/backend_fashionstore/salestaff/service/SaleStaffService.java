package com.example.backend_fashionstore.salestaff.service;

import com.example.backend_fashionstore.QuanLyDonHang.dto.OrderStatisticsDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.*;
import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderDetailRepository;
import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderRepository;
import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import com.example.backend_fashionstore.customer.order.repository.OrderHistoryRepository;
import com.example.backend_fashionstore.customer.order.repository.ReturnRequestRepository;
import com.example.backend_fashionstore.customer.order.service.CustomerOrderServiceImpl;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.enums.OrderStatus;
import com.example.backend_fashionstore.enums.PaymentStatus;
import com.example.backend_fashionstore.enums.UserStatus;
import com.example.backend_fashionstore.quanLyKhachHang.dto.CustomerDTO;
import com.example.backend_fashionstore.quanLyKhachHang.repository.KhachHangCustomerRepository;
import com.example.backend_fashionstore.quanLyVoucher.repository.VoucherRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SaleStaffService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ProductDetailRepository productDetailRepository;
    private final CustomerOrderServiceImpl customerOrderService;
    private final VoucherRepository voucherRepository;
    private final KhachHangCustomerRepository customerRepository;
    private final com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository.PosOrderRepository posOrderRepository;

    // =============================================
    // QUẢN LÝ ĐƠN HÀNG (FULL CHỨC NĂNG)
    // =============================================

    public Page<AdminOrderResponseDTO> getAllOrders(String status, String keyword, Pageable pageable) {
        Page<Order> orders;

        log.info("getAllOrders - status: {}, keyword: {}, pageable: {}", status, keyword, pageable);

        if ("REJECTED".equals(status)) {
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

    public AdminOrderResponseDTO getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
        return convertToAdminResponse(order);
    }

    public AdminOrderResponseDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        String oldStatus = order.getOrderStatus().toString();
        String newStatus = request.getOrderStatus();

        // ✅ Lấy thông tin người dùng hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        // ✅ Lấy email từ authentication
        String currentEmail = authentication.getPrincipal() instanceof UserDetails
                ? ((UserDetails) authentication.getPrincipal()).getUsername()
                : currentUser;

        String currentRole = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse("UNKNOWN");

        log.info("🔐 Current user: {}, Email: {}, Role: {}", currentUser, currentEmail, currentRole);
        log.info("📋 Order {} - Old Status: {}, New Status: {}, ProcessedBy: {}",
                orderId, oldStatus, newStatus, order.getProcessedBy());

        // ✅ QUAN TRỌNG: Kiểm tra nếu đơn hàng đã có người xác nhận
        if (order.getProcessedBy() != null && !order.getProcessedBy().isEmpty()) {
            // ✅ Nếu đây là hành động xác nhận (PENDING_PAYMENT -> CONFIRMED) nhưng đã có người xác nhận
            if (isConfirmationAction(oldStatus, newStatus)) {
                String errorMsg = String.format(
                        "❌ Đơn hàng #%d đã được xác nhận bởi: %s (Email: %s, Role: %s) vào lúc: %s\n" +
                                "Không thể xác nhận lại đơn hàng này!",
                        orderId,
                        order.getProcessedBy(),
                        order.getProcessedByEmail() != null ? order.getProcessedByEmail() : "Không có email",
                        order.getProcessedByRole(),
                        order.getProcessedAt() != null ? order.getProcessedAt().toString() : "không rõ"
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            // ✅ Nếu đơn hàng đã được xác nhận, chỉ người xác nhận mới được cập nhật tiếp
            if (!isConfirmationAction(oldStatus, newStatus) && !order.getProcessedBy().equals(currentUser)) {
                String errorMsg = String.format(
                        "❌ Đơn hàng #%d đã được xác nhận bởi: %s (Email: %s, Role: %s) vào lúc: %s\n" +
                                "Chỉ %s mới có quyền cập nhật đơn hàng này!",
                        orderId,
                        order.getProcessedBy(),
                        order.getProcessedByEmail() != null ? order.getProcessedByEmail() : "Không có email",
                        order.getProcessedByRole(),
                        order.getProcessedAt() != null ? order.getProcessedAt().toString() : "không rõ",
                        order.getProcessedBy()
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
        }

        // ✅ Nếu chưa có người xác nhận và đây là hành động xác nhận -> lưu thông tin người xác nhận
        if (isConfirmationAction(oldStatus, newStatus)) {
            // ✅ Lưu thông tin người xác nhận (bao gồm email)
            order.setProcessedBy(currentUser);
            order.setProcessedByEmail(currentEmail);
            order.setProcessedByRole(currentRole);
            order.setProcessedAt(LocalDateTime.now());

            log.info("✅ Order {} confirmed by: {} (Email: {}, Role: {}) at {}",
                    orderId, currentUser, currentEmail, currentRole, LocalDateTime.now());
        }

        // Kiểm tra chuyển trạng thái hợp lệ
        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new RuntimeException("Không thể chuyển trạng thái từ '" + oldStatus + "' sang '" + newStatus + "'");
        }

        if ("PROCESSING".equals(oldStatus) && "SHIPPING".equals(newStatus)) {
            try {
                customerOrderService.deductStockForOrder(orderId);
            } catch (Exception e) {
                log.error("❌ Failed to deduct stock for order {}: {}", orderId, e.getMessage());
                throw new RuntimeException("Không thể trừ số lượng sản phẩm trong kho: " + e.getMessage());
            }
        }

        if ("COMPLETED".equals(newStatus)) {
            if (order.getPaymentStatus() != PaymentStatus.SUCCESS) {
                order.setPaymentStatus(PaymentStatus.SUCCESS);
                OrderHistory paymentHistory = new OrderHistory();
                paymentHistory.setOrder(order);
                paymentHistory.setOrderStatus(oldStatus);
                paymentHistory.setNote("Hệ thống tự động cập nhật trạng thái thanh toán thành 'SUCCESS' do đơn hàng đã 'COMPLETED'");
                paymentHistory.setTimestamp(LocalDateTime.now());
                orderHistoryRepository.save(paymentHistory);
                log.info("✅ Auto updated payment status to SUCCESS for order {} because it is COMPLETED", orderId);
            }
        }

        // Cập nhật trạng thái
        order.setOrderStatus(OrderStatus.valueOf(newStatus));
        order.setUpdatedAt(LocalDateTime.now());

        if ("RETURN_RECEIVED".equals(oldStatus) && "RETURNED".equals(newStatus)) {
            returnRequestRepository.findFirstByOrderIdAndDeletedAtIsNull(orderId)
                    .ifPresent(returnRequest -> {
                        returnRequest.setStatus("COMPLETED");
                        returnRequest.setUpdatedAt(LocalDateTime.now());
                        returnRequestRepository.save(returnRequest);
                        log.info("✅ Return request completed for order {}", orderId);
                    });
        }

        if ("CANCELLED".equals(newStatus) && isStockDeducted(oldStatus)) {
            restoreStockForOrder(orderId);
        }

        orderRepository.save(order);

        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus(newStatus);
        String note = "Cập nhật trạng thái từ '" + getStatusText(oldStatus) + "' thành '" + getStatusText(newStatus) + "'";
        if (request.getNotes() != null) {
            note += ". " + request.getNotes();
        }
        // ✅ Thêm thông tin người xác nhận vào lịch sử
        if (order.getProcessedBy() != null) {
            note += " (Xác nhận bởi: " + order.getProcessedBy()
                    + " - Email: " + (order.getProcessedByEmail() != null ? order.getProcessedByEmail() : "N/A")
                    + " - " + order.getProcessedByRole() + ")";
        }
        history.setNote(note);
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Updated order {} status from {} to {}", orderId, oldStatus, newStatus);

        return convertToAdminResponse(order);
    }

    // Sửa trong cả AdminOrderServiceImpl và SaleStaffService

    private boolean isConfirmationAction(String oldStatus, String newStatus) {
        // ✅ Chỉ PENDING_PAYMENT -> CONFIRMED mới là hành động xác nhận
        // CONFIRMED -> PROCESSING là cập nhật trạng thái, KHÔNG phải xác nhận
        return "PENDING_PAYMENT".equals(oldStatus) && "CONFIRMED".equals(newStatus);
    }

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

    @Transactional
    public AdminOrderResponseDTO adminCancelOrder(Long orderId, String reason) {
        log.info("🗑️ Cancelling order: {}, reason: {}", orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        String oldStatus = order.getOrderStatus().toString();

        if ("CANCELLED".equals(oldStatus)) {
            throw new RuntimeException("Đơn hàng đã bị hủy trước đó");
        }

        if ("COMPLETED".equals(oldStatus)) {
            throw new RuntimeException("Không thể hủy đơn hàng đã hoàn thành");
        }

        if (isStockDeducted(oldStatus)) {
            restoreStockForOrder(orderId);
        }

        Voucher voucher = order.getVoucher();
        if (voucher != null) {
            voucher.setVoucherUsedCount(Math.max(0, voucher.getVoucherUsedCount() - 1));
            voucherRepository.save(voucher);
            log.info("🔄 Restored voucher usage: {}", voucher.getVoucherCode());
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus("CANCELLED");
        history.setNote("Hủy đơn hàng. Lý do: " + reason);
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Order {} cancelled. Old status: {}", orderId, oldStatus);

        return convertToAdminResponse(order);
    }

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

        long returnRequested = orderRepository.countByOrderStatus(OrderStatus.RETURN_REQUESTED);
        long returnApproved = orderRepository.countByOrderStatus(OrderStatus.RETURN_APPROVED);
        long returnReceived = orderRepository.countByOrderStatus(OrderStatus.RETURN_RECEIVED);
        long returnPending = orderRepository.countByOrderStatus(OrderStatus.RETURN_PENDING);
        long returnCompleted = orderRepository.countByOrderStatus(OrderStatus.RETURNED);
        long returnRejected = orderRepository.countByOrderStatus(OrderStatus.REJECTED);

        BigDecimal todayRevenue = orderRepository.getTodayRevenue();
        if (todayRevenue == null) todayRevenue = BigDecimal.ZERO;
        BigDecimal todayPosRevenue = posOrderRepository.getTodayPosRevenue();
        if (todayPosRevenue != null) todayRevenue = todayRevenue.add(todayPosRevenue);

        BigDecimal monthlyRevenue = orderRepository.getMonthlyRevenue(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue()
        );
        if (monthlyRevenue == null) monthlyRevenue = BigDecimal.ZERO;
        BigDecimal monthlyPosRevenue = posOrderRepository.getCurrentMonthPosRevenue();
        if (monthlyPosRevenue != null) monthlyRevenue = monthlyRevenue.add(monthlyPosRevenue);

        BigDecimal totalRevenue = orderRepository.getTotalRevenue();
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        
        BigDecimal totalPosRevenue = posOrderRepository.getTotalRevenue(
                LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.now()
        );
        if (totalPosRevenue != null) totalRevenue = totalRevenue.add(totalPosRevenue);

        BigDecimal onlineRevenue = orderRepository.getOnlineRevenue();
        if (onlineRevenue == null) onlineRevenue = BigDecimal.ZERO;

        BigDecimal returnRefundAmount = orderRepository.getReturnRefundAmount();
        if (returnRefundAmount == null) returnRefundAmount = BigDecimal.ZERO;

        BigDecimal netRevenue = onlineRevenue.subtract(returnRefundAmount);
        if (netRevenue.compareTo(BigDecimal.ZERO) < 0) netRevenue = BigDecimal.ZERO;

        long cancelledByCustomer = orderRepository.countCancelledByType("Khách hàng hủy");
        long cancelledByAdmin = orderRepository.countCancelledByType("Admin hủy");
        long cancelledBySystem = orderRepository.countCancelledByType("Hệ thống hủy");

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
                .returnRequested(returnRequested)
                .returnApproved(returnApproved)
                .returnReceived(returnReceived)
                .returnPending(returnPending)
                .returnCompleted(returnCompleted)
                .returnRejected(returnRejected)
                .todayRevenue(todayRevenue)
                .monthlyRevenue(monthlyRevenue)
                .totalRevenue(totalRevenue)
                .onlineRevenue(onlineRevenue)
                .returnRefundAmount(returnRefundAmount)
                .netRevenue(netRevenue)
                .cancelledByCustomer(cancelledByCustomer)
                .cancelledByAdmin(cancelledByAdmin)
                .cancelledBySystem(cancelledBySystem)
                .build();
    }

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

    // =============================================
    // QUẢN LÝ HOÀN HÀNG
    // =============================================

    public ReturnRequestDetailDTO getReturnRequestByOrderId(Long orderId) {
        ReturnRequest returnRequest = returnRequestRepository
                .findFirstByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn hàng"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

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

    @Transactional
    public ReturnRequestDetailDTO processReturnRequest(Long orderId, String status, String adminNote) {
        ReturnRequest returnRequest = returnRequestRepository
                .findFirstByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn hàng"));

        if (!"PENDING".equals(returnRequest.getStatus())) {
            throw new RuntimeException("Yêu cầu hoàn hàng đã được xử lý trước đó");
        }

        returnRequest.setStatus(status);
        if (adminNote != null && !adminNote.isEmpty()) {
            String currentNote = returnRequest.getAdminNote();
            returnRequest.setAdminNote(currentNote != null ? currentNote + "\n" + adminNote : adminNote);
        }
        returnRequest.setUpdatedAt(LocalDateTime.now());
        returnRequestRepository.save(returnRequest);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if ("APPROVED".equals(status)) {
            order.setOrderStatus(OrderStatus.RETURN_APPROVED);
        } else if ("REJECTED".equals(status)) {
            order.setOrderStatus(OrderStatus.REJECTED);
        } else {
            throw new RuntimeException("Trạng thái không hợp lệ. Chỉ chấp nhận APPROVED hoặc REJECTED");
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus(order.getOrderStatus().toString());
        String actionText = "APPROVED".equals(status) ? "duyệt" : "từ chối";
        String note = actionText + " yêu cầu hoàn hàng";
        if (adminNote != null && !adminNote.isEmpty()) {
            note += ". Phản hồi: " + adminNote;
        }
        history.setNote(note);
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Return request processed for order {}: {}", orderId, status);

        return getReturnRequestByOrderId(orderId);
    }

    @Transactional
    public ReturnRequestDetailDTO receiveReturnedItems(Long orderId, String note) {
        ReturnRequest returnRequest = returnRequestRepository
                .findFirstByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn hàng"));

        if (!"APPROVED".equals(returnRequest.getStatus())) {
            throw new RuntimeException("Chỉ có thể xác nhận nhận hàng khi yêu cầu đã được duyệt");
        }

        returnRequest.setStatus("RECEIVED");
        if (note != null && !note.isEmpty()) {
            String adminNote = returnRequest.getAdminNote() != null ?
                    returnRequest.getAdminNote() + "\n" + note : note;
            returnRequest.setAdminNote(adminNote);
        }
        returnRequest.setUpdatedAt(LocalDateTime.now());
        returnRequestRepository.save(returnRequest);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        order.setOrderStatus(OrderStatus.RETURN_RECEIVED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus("RETURN_RECEIVED");
        String historyNote = "Xác nhận đã nhận hàng trả";
        if (note != null && !note.isEmpty()) {
            historyNote += ". Ghi chú: " + note;
        }
        history.setNote(historyNote);
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Returned items received for order {}", orderId);

        return getReturnRequestByOrderId(orderId);
    }

    public Page<ReturnRequestDetailDTO> getAllReturnRequests(String status, Pageable pageable) {
        Page<ReturnRequest> requests;
        if (status != null && !status.isEmpty()) {
            requests = returnRequestRepository.findByStatusAndDeletedAtIsNull(status, pageable);
        } else {
            requests = returnRequestRepository.findByDeletedAtIsNull(pageable);
        }

        return requests.map(r -> {
            Order order = orderRepository.findById(r.getOrderId()).orElse(null);
            ReturnRequestDetailDTO dto = getReturnRequestByOrderId(r.getOrderId());
            return dto;
        });
    }

    public InvoiceDTO getInvoice(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

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

        String shippingAddressStr = order.getShippingAddress();
        String shippingAddress = "";
        String shippingCity = "";
        String shippingDistrict = "";
        String shippingWard = "";
        String shippingFullName = "";
        String shippingPhone = "";
        String shippingFullAddress = "";

        if (shippingAddressStr != null && !shippingAddressStr.isEmpty()) {
            Customer customer = order.getCustomer();
            if (customer != null) {
                shippingFullName = customer.getCustomerName() != null ? customer.getCustomerName() : "";
                shippingPhone = customer.getPhone() != null ? customer.getPhone() : "";
            }
            shippingAddress = shippingAddressStr;
            shippingCity = order.getShippingCity() != null ? order.getShippingCity() : "";
            shippingDistrict = order.getShippingDistrict() != null ? order.getShippingDistrict() : "";
            shippingWard = order.getShippingWard() != null ? order.getShippingWard() : "";
            shippingFullAddress = shippingAddressStr;
        } else {
            Customer customer = order.getCustomer();
            if (customer != null) {
                shippingFullName = customer.getCustomerName() != null ? customer.getCustomerName() : "";
                shippingPhone = customer.getPhone() != null ? customer.getPhone() : "";
                shippingAddress = customer.getAddress() != null ? customer.getAddress() : "";
                shippingFullAddress = customer.getAddress();
            }
        }

        Voucher voucher = order.getVoucher();
        String voucherCode = null;
        BigDecimal voucherDiscountAmount = null;
        if (voucher != null) {
            voucherCode = voucher.getVoucherCode();
            voucherDiscountAmount = voucher.getVoucherDiscountAmount();
        }

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
                .shippingFullAddress(shippingFullAddress)
                .voucherCode(voucherCode)
                .voucherDiscountAmount(voucherDiscountAmount)
                .items(items)
                .build();
    }

    // =============================================
    // QUẢN LÝ SẢN PHẨM
    // =============================================

    @Transactional
    public ProductDetail updateStock(Integer productDetailId, Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new RuntimeException("Số lượng phải lớn hơn hoặc bằng 0");
        }

        ProductDetail productDetail = productDetailRepository.findById(productDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm"));

        if (productDetail.getDeletedAt() != null || !"ACTIVE".equals(productDetail.getStatus())) {
            throw new RuntimeException("Sản phẩm này đã bị vô hiệu hóa hoặc xóa");
        }

        int oldQuantity = productDetail.getQuantity();
        productDetail.setQuantity(quantity);
        productDetail.setUpdatedAt(LocalDateTime.now());

        ProductDetail saved = productDetailRepository.save(productDetail);
        log.info("🔄 Updated stock: productDetailId={}, old={}, new={}",
                productDetailId, oldQuantity, quantity);

        return saved;
    }

    @Transactional
    public ProductDetail increaseStock(Integer productDetailId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Số lượng tăng phải lớn hơn 0");
        }

        ProductDetail productDetail = productDetailRepository.findById(productDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm"));

        if (productDetail.getDeletedAt() != null || !"ACTIVE".equals(productDetail.getStatus())) {
            throw new RuntimeException("Sản phẩm này đã bị vô hiệu hóa hoặc xóa");
        }

        int newQuantity = productDetail.getQuantity() + quantity;
        productDetail.setQuantity(newQuantity);
        productDetail.setUpdatedAt(LocalDateTime.now());

        ProductDetail saved = productDetailRepository.save(productDetail);
        log.info("📈 Increased stock: productDetailId={}, added={}, new={}",
                productDetailId, quantity, newQuantity);

        return saved;
    }

    @Transactional
    public ProductDetail decreaseStock(Integer productDetailId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Số lượng giảm phải lớn hơn 0");
        }

        ProductDetail productDetail = productDetailRepository.findById(productDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm"));

        if (productDetail.getDeletedAt() != null || !"ACTIVE".equals(productDetail.getStatus())) {
            throw new RuntimeException("Sản phẩm này đã bị vô hiệu hóa hoặc xóa");
        }

        if (productDetail.getQuantity() < quantity) {
            throw new RuntimeException("Số lượng tồn kho không đủ. Hiện có: " + productDetail.getQuantity());
        }

        int newQuantity = productDetail.getQuantity() - quantity;
        productDetail.setQuantity(newQuantity);
        productDetail.setUpdatedAt(LocalDateTime.now());

        ProductDetail saved = productDetailRepository.save(productDetail);
        log.info("📉 Decreased stock: productDetailId={}, removed={}, new={}",
                productDetailId, quantity, newQuantity);

        return saved;
    }

    public ProductDetail getProductDetail(Integer productDetailId) {
        return productDetailRepository.findByIdWithRelations(productDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm"));
    }

    // =============================================
    // QUẢN LÝ KHÁCH HÀNG
    // =============================================

    public Page<CustomerDTO> getCustomers(String keyword, String statusParam, Pageable pageable) {
        UserStatus status = null;
        if (statusParam != null && !statusParam.isEmpty()) {
            if (statusParam.equalsIgnoreCase("ACTIVE")) status = UserStatus.ACTIVE;
            else if (statusParam.equalsIgnoreCase("LOCKED")) status = UserStatus.BANNED;
            else if (statusParam.equalsIgnoreCase("DELETED")) status = UserStatus.DELETED;
        }

        Page<Customer> customers;
        boolean hasKeyword = (keyword != null && !keyword.isEmpty());

        if (status != null) {
            if (hasKeyword) {
                customers = customerRepository.searchCustomersByStatus(keyword, status, pageable);
            } else {
                customers = customerRepository.findAllByStatus(status, pageable);
            }
        } else {
            if (hasKeyword) {
                customers = customerRepository.searchCustomers(keyword, pageable);
            } else {
                customers = customerRepository.findAllActiveCustomers(pageable);
            }
        }
        return customers.map(this::convertToCustomerDTO);
    }

    public CustomerDTO getCustomerById(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        return convertToCustomerDTO(customer);
    }

    private CustomerDTO convertToCustomerDTO(Customer c) {
        CustomerDTO dto = new CustomerDTO();
        dto.setCustomerId(c.getCustomerId());
        dto.setUserId(c.getUser().getUserId());
        dto.setEmail(c.getUser().getEmail());
        dto.setUsername(c.getUser().getUsername());
        dto.setCustomerName(c.getCustomerName());
        dto.setPhone(c.getPhone());
        dto.setAddress(c.getAddress());
        dto.setIsLocked(c.getUser().getStatus() == UserStatus.BANNED);
        dto.setIsActive(c.getUser().getStatus() == UserStatus.ACTIVE);
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }

    // =============================================
    // HELPER METHODS
    // =============================================

    private boolean isValidStatusTransition(String oldStatus, String newStatus) {
        if ("CANCELLED".equals(newStatus)) {
            return false;
        }

        if ("RETURN_APPROVED".equals(oldStatus) && "RETURN_RECEIVED".equals(newStatus)) {
            return true;
        }

        if ("RETURN_RECEIVED".equals(oldStatus) && "RETURNED".equals(newStatus)) {
            return true;
        }

        if ("PROCESSING".equals(oldStatus) && "SHIPPING".equals(newStatus)) {
            return true;
        }

        List<String> validSequence = Arrays.asList(
                "PENDING_PAYMENT", "CONFIRMED", "PROCESSING", "SHIPPING", "DELIVERED", "COMPLETED"
        );
        int oldIndex = validSequence.indexOf(oldStatus);
        int newIndex = validSequence.indexOf(newStatus);

        return newIndex == oldIndex + 1;
    }

    private boolean isStockDeducted(String orderStatus) {
        return "SHIPPING".equals(orderStatus) ||
                "DELIVERED".equals(orderStatus) ||
                "COMPLETED".equals(orderStatus) ||
                "RETURN_REQUESTED".equals(orderStatus) ||
                "RETURN_APPROVED".equals(orderStatus) ||
                "RETURN_RECEIVED".equals(orderStatus) ||
                "RETURN_PENDING".equals(orderStatus) ||
                "RETURNED".equals(orderStatus);
    }

    private void restoreStockForOrder(Long orderId) {
        log.info("📦 Restoring stock for order: {}", orderId);
        List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(orderId);
        for (OrderDetail detail : details) {
            ProductDetail pd = detail.getProductDetail();
            pd.setQuantity(pd.getQuantity() + detail.getQuantity());
            productDetailRepository.save(pd);
            log.info("✅ Restored {} units for product detail: {}", detail.getQuantity(), pd.getProductDetailId());
        }
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

    private String buildFullAddress(String streetAddress, String ward, String district, String city) {
        StringBuilder sb = new StringBuilder();
        if (streetAddress != null && !streetAddress.isEmpty()) {
            sb.append(streetAddress);
        }
        if (ward != null && !ward.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ward);
        }
        if (district != null && !district.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(district);
        }
        if (city != null && !city.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    // =============================================
    // CHUYỂN ĐỔI ORDER -> ADMIN RESPONSE
    // =============================================

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

        String shippingAddressStr = order.getShippingAddress();
        AdminShippingAddressDTO shippingDTO = null;

        String shippingPhone = null;
        String shippingFullName = null;
        String shippingFullAddress = shippingAddressStr;

        if (shippingAddressStr != null && !shippingAddressStr.isEmpty()) {
            Customer customer = order.getCustomer();
            if (customer != null) {
                shippingFullName = customer.getCustomerName();
                shippingPhone = customer.getPhone();
            }

            String city = order.getShippingCity() != null ? order.getShippingCity() : "";
            String district = order.getShippingDistrict() != null ? order.getShippingDistrict() : "";
            String ward = order.getShippingWard() != null ? order.getShippingWard() : "";
            shippingFullAddress = shippingAddressStr;

            shippingDTO = AdminShippingAddressDTO.builder()
                    .addressId(null)
                    .fullName(shippingFullName)
                    .phone(shippingPhone)
                    .addressLine(shippingAddressStr)
                    .city(city)
                    .district(district)
                    .ward(ward)
                    .fullAddress(shippingFullAddress)
                    .build();
        } else {
            Customer customer = order.getCustomer();
            if (customer != null) {
                shippingFullName = customer.getCustomerName();
                shippingPhone = customer.getPhone();
                shippingFullAddress = customer.getAddress();

                shippingDTO = AdminShippingAddressDTO.builder()
                        .addressId(null)
                        .fullName(shippingFullName)
                        .phone(shippingPhone)
                        .addressLine(customer.getAddress())
                        .city("")
                        .district("")
                        .ward("")
                        .fullAddress(shippingFullAddress)
                        .build();
            }
        }

        String displayPhone = shippingPhone;
        if (displayPhone == null || displayPhone.isEmpty()) {
            Customer customer = order.getCustomer();
            if (customer != null) {
                displayPhone = customer.getPhone();
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

        String customerName = order.getCustomer() != null ? order.getCustomer().getCustomerName() : "Khách vãng lai";

        String customerEmail = null;
        if (order.getCustomer() != null && order.getCustomer().getUser() != null) {
            customerEmail = order.getCustomer().getUser().getEmail();
        }

        // ✅ Lấy thông tin người xác nhận
        String processedBy = order.getProcessedBy();
        String processedByEmail = order.getProcessedByEmail();
        String processedByRole = order.getProcessedByRole();
        LocalDateTime processedAt = order.getProcessedAt();

        if (processedBy != null) {
            log.debug("📋 Order {} processed by: {} (Email: {}, Role: {}) at {}",
                    order.getOrderId(), processedBy, processedByEmail, processedByRole, processedAt);
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
                        .customerId(order.getCustomer() != null ? order.getCustomer().getCustomerId() : null)
                        .customerName(customerName)
                        .phone(displayPhone)
                        .email(customerEmail)
                        .build())
                .shippingAddress(shippingDTO)
                .voucher(voucherDTO)
                .items(itemResponses)
                .processedBy(processedBy)
                .processedByEmail(processedByEmail)
                .processedByRole(processedByRole)
                .processedAt(processedAt)
                .build();
    }
}