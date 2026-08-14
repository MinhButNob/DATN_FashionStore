package com.example.backend_fashionstore.QuanLyDonHang.service;

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
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ProductDetailRepository productDetailRepository;
    private final CustomerOrderServiceImpl customerOrderService;
    private final VoucherRepository voucherRepository;
    private final com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository.PosOrderRepository posOrderRepository;

    // ================================================================
    // ===== ĐƠN HÀNG =====
    // ================================================================

    @Override
    public Page<AdminOrderResponseDTO> getAllOrders(String status, LocalDateTime fromDate, LocalDateTime toDate, String keyword, Pageable pageable) {
        Page<Order> orders;

        log.info("getAllOrders - status: {}, fromDate: {}, toDate: {}, keyword: {}, pageable: {}",
                status, fromDate, toDate, keyword, pageable);

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

        // ✅ Lấy thông tin người dùng hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        String currentEmail = authentication.getPrincipal() instanceof UserDetails
                ? ((UserDetails) authentication.getPrincipal()).getUsername()
                : currentUser;
        String currentRole = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse("UNKNOWN");

        log.info("🔐 Current user: {}, Email: {}, Role: {}", currentUser, currentEmail, currentRole);
        log.info("📋 Order {} - Old Status: {}, New Status: {}, ProcessedBy: {}, ProcessedByEmail: {}",
                orderId, oldStatus, newStatus, order.getProcessedBy(), order.getProcessedByEmail());

        // ✅ CHỈ PENDING_PAYMENT -> CONFIRMED mới là hành động xác nhận
        boolean isConfirmAction = "PENDING_PAYMENT".equals(oldStatus) && "CONFIRMED".equals(newStatus);

        // ✅ QUAN TRỌNG: Kiểm tra nếu đơn hàng đã có người xác nhận
        if (order.getProcessedBy() != null && !order.getProcessedBy().isEmpty()) {
            // ✅ Nếu đây là hành động xác nhận nhưng đã có người xác nhận
            if (isConfirmAction) {
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

            // ✅ Nếu đơn hàng đã được xác nhận, CHỈ người xác nhận mới được cập nhật
            boolean isSameUser = false;

            // So sánh username
            if (order.getProcessedBy() != null && order.getProcessedBy().equalsIgnoreCase(currentUser)) {
                isSameUser = true;
            }

            // So sánh email
            if (order.getProcessedByEmail() != null && currentEmail != null) {
                if (order.getProcessedByEmail().equalsIgnoreCase(currentEmail)) {
                    isSameUser = true;
                }
            }

            // Nếu processedBy trùng với email current
            if (order.getProcessedBy() != null && currentEmail != null) {
                if (order.getProcessedBy().equalsIgnoreCase(currentEmail)) {
                    isSameUser = true;
                }
            }

            // Nếu processedByEmail trùng với username current
            if (order.getProcessedByEmail() != null && order.getProcessedByEmail().equalsIgnoreCase(currentUser)) {
                isSameUser = true;
            }

            if (!isSameUser) {
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
        if (isConfirmAction) {
            order.setProcessedBy(currentUser);
            order.setProcessedByEmail(currentEmail);
            order.setProcessedByRole(currentRole);
            order.setProcessedAt(LocalDateTime.now());

            log.info("✅ Order {} confirmed by: {} (Email: {}, Role: {}) at {}",
                    orderId, currentUser, currentEmail, currentRole, LocalDateTime.now());
        }

        // Kiểm tra chuyển trạng thái hợp lệ
        if (!isValidAdminStatusTransition(oldStatus, newStatus)) {
            throw new RuntimeException("Không thể chuyển trạng thái từ '" + oldStatus + "' sang '" + newStatus + "'");
        }

        // ✅ Xử lý trừ stock khi chuyển từ PROCESSING -> SHIPPING
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
                paymentHistory.setOrderStatus(oldStatus); // keep current status for this history entry
                paymentHistory.setNote("Hệ thống tự động cập nhật trạng thái thanh toán thành 'SUCCESS' do đơn hàng đã 'COMPLETED'");
                paymentHistory.setTimestamp(LocalDateTime.now());
                orderHistoryRepository.save(paymentHistory);
                log.info("✅ Auto updated payment status to SUCCESS for order {} because it is COMPLETED", orderId);
            }
        }
        // ✅ Cập nhật trạng thái đơn hàng
        order.setOrderStatus(OrderStatus.valueOf(newStatus));
        order.setUpdatedAt(LocalDateTime.now());

        // ✅ 🔥 QUAN TRỌNG: Khi chuyển sang DELIVERED, tự động cập nhật paymentStatus = SUCCESS
        if ("DELIVERED".equals(newStatus)) {
            order.setPaymentStatus(PaymentStatus.SUCCESS);
            log.info("✅ Order {} delivered, payment status automatically updated to SUCCESS", orderId);
        }

        // ✅ Xử lý hoàn hàng: Khi chuyển từ RETURN_RECEIVED -> RETURNED
        if ("RETURN_RECEIVED".equals(oldStatus) && "RETURNED".equals(newStatus)) {
            returnRequestRepository.findFirstByOrderIdAndDeletedAtIsNull(orderId)
                    .ifPresent(returnRequest -> {
                        // ✅ TÍNH TOÁN TIỀN HOÀN (REFUND AMOUNT) KHI HOÀN TẤT
                        try {
                            if (returnRequest.getQuantities() != null && !returnRequest.getQuantities().isEmpty()) {
                                ObjectMapper mapper = new ObjectMapper();
                                Map<String, Integer> quantities = mapper.readValue(returnRequest.getQuantities(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Integer>>() {});
                                BigDecimal totalRefund = BigDecimal.ZERO;

                                List<OrderDetail> orderDetails = orderDetailRepository.findByOrder_OrderId(orderId);

                                for (OrderDetail detail : orderDetails) {
                                    String productIdStr = String.valueOf(detail.getProductDetail().getProductDetailId());
                                    if (quantities.containsKey(productIdStr)) {
                                        int returnQty = quantities.get(productIdStr);
                                        if (returnQty > 0) {
                                            // Tiền hoàn cho 1 sản phẩm = finalAmount / số lượng mua
                                            BigDecimal unitFinalPrice = detail.getFinalAmount().divide(BigDecimal.valueOf(detail.getQuantity()), 2, java.math.RoundingMode.HALF_UP);
                                            totalRefund = totalRefund.add(unitFinalPrice.multiply(BigDecimal.valueOf(returnQty)));
                                        }
                                    }
                                }

                                order.setRefundAmount(totalRefund);
                                log.info("💰 Đã tính tiền hoàn cho đơn hàng {} khi HOÀN TẤT: {}", orderId, totalRefund);
                            }
                        } catch (Exception e) {
                            log.error("❌ Lỗi khi tính toán tiền hoàn cho đơn hàng {}: {}", orderId, e.getMessage());
                        }

                        returnRequest.setStatus("COMPLETED");
                        returnRequest.setUpdatedAt(LocalDateTime.now());
                        returnRequestRepository.save(returnRequest);
                        log.info("✅ Return request completed for order {}", orderId);
                    });
        }

        // ✅ Xử lý hủy đơn: Nếu chuyển sang CANCELLED và đã trừ stock thì hoàn lại
        if ("CANCELLED".equals(newStatus) && isStockDeducted(oldStatus)) {
            restoreStockForOrder(orderId);
        }

        orderRepository.save(order);

        // ✅ Lưu lịch sử
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus(newStatus);
        String note = "Admin cập nhật trạng thái từ '" + getStatusText(oldStatus) + "' thành '" + getStatusText(newStatus) + "'";

        // ✅ Thêm thông tin thanh toán nếu được cập nhật tự động
        if ("DELIVERED".equals(newStatus)) {
            note += " (Thanh toán tự động chuyển thành THÀNH CÔNG khi nhận hàng)";
        }

        if (request.getNotes() != null) {
            note += ". " + request.getNotes();
        }
        if (order.getProcessedBy() != null) {
            note += " (Xác nhận bởi: " + order.getProcessedBy()
                    + " - Email: " + (order.getProcessedByEmail() != null ? order.getProcessedByEmail() : "N/A")
                    + " - " + order.getProcessedByRole() + ")";
        }
        history.setNote(note);
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Admin updated order {} status from {} to {}", orderId, oldStatus, newStatus);

        // ✅ Gửi thông báo cho khách hàng
        try {
            sendOrderStatusNotification(order, oldStatus, newStatus, request.getNotes());
        } catch (Exception e) {
            log.warn("⚠️ Không thể gửi thông báo cho khách hàng: {}", e.getMessage());
        }

        return convertToAdminResponse(order);
    }

    /**
     * Khôi phục số lượng tồn kho khi hủy đơn hàng
     */
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

    // ===== 🆕 DOANH THU THEO THỜI GIAN (dùng cho biểu đồ "Thống kê doanh thu theo thời gian") =====
    @Override
    public java.util.List<java.util.Map<String, Object>> getRevenueByTime(String type, Integer year, Integer month) {
        int y = (year != null) ? year : LocalDateTime.now().getYear();
        java.util.List<Object[]> rows;
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

        if ("daily".equalsIgnoreCase(type)) {
            int m = (month != null) ? month : LocalDateTime.now().getMonthValue();
            rows = orderRepository.getDailyRevenueByMonth(y, m);

            // Map dữ liệu trả về theo ngày, index theo số ngày để FE dễ vẽ chart (không bị lệch nếu ngày nào ko có đơn)
            java.util.Map<Integer, BigDecimal> revenueByDay = new java.util.HashMap<>();
            for (Object[] row : rows) {
                int day = ((Number) row[0]).intValue();
                BigDecimal revenue = (BigDecimal) row[1];
                revenueByDay.put(day, revenue);
            }
            int daysInMonth = java.time.YearMonth.of(y, m).lengthOfMonth();
            for (int d = 1; d <= daysInMonth; d++) {
                java.util.Map<String, Object> point = new java.util.LinkedHashMap<>();
                point.put("label", "Ngày " + d);
                point.put("revenue", revenueByDay.getOrDefault(d, BigDecimal.ZERO));
                result.add(point);
            }
        } else {
            // "monthly" (mặc định): doanh thu theo từng tháng trong năm
            rows = orderRepository.getMonthlyRevenueByYear(y);

            java.util.Map<Integer, BigDecimal> revenueByMonth = new java.util.HashMap<>();
            for (Object[] row : rows) {
                int m = ((Number) row[0]).intValue();
                BigDecimal revenue = (BigDecimal) row[1];
                revenueByMonth.put(m, revenue);
            }
            for (int m = 1; m <= 12; m++) {
                java.util.Map<String, Object> point = new java.util.LinkedHashMap<>();
                point.put("label", "Tháng " + m);
                point.put("revenue", revenueByMonth.getOrDefault(m, BigDecimal.ZERO));
                result.add(point);
            }
        }

        return result;
    }

    // ================================================================
    // ===== QUẢN LÝ HOÀN HÀNG =====
    // ================================================================

    @Override
    public ReturnRequestDetailDTO getReturnRequestByOrderId(Long orderId) {
        log.info("📋 Getting return request for order: {}", orderId);

        ReturnRequest returnRequest = returnRequestRepository
                .findFirstByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn hàng cho đơn hàng này"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        return convertToReturnRequestDetailDTO(returnRequest, order);
    }

    @Override
    @Transactional
    public ReturnRequestDetailDTO processReturnRequest(Long orderId, String status, String adminNote) {
        log.info("🔄 Processing return request for order: {}, status: {}", orderId, status);

        ReturnRequest returnRequest = returnRequestRepository
                .findFirstByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn hàng"));

        if ("REJECTED".equals(status)) {
            if ("COMPLETED".equals(returnRequest.getStatus()) || "REJECTED".equals(returnRequest.getStatus())) {
                throw new RuntimeException("Yêu cầu hoàn hàng đã kết thúc, không thể từ chối");
            }
        } else if ("APPROVED".equals(status)) {
            if (!"PENDING".equals(returnRequest.getStatus())) {
                throw new RuntimeException("Yêu cầu hoàn hàng đã được xử lý trước đó");
            }
        } else {
            throw new RuntimeException("Trạng thái không hợp lệ. Chỉ chấp nhận APPROVED hoặc REJECTED");
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
            log.info("✅ Return request APPROVED for order {}", orderId);
        } else if ("REJECTED".equals(status)) {
            order.setOrderStatus(OrderStatus.REJECTED);
            log.info("❌ Return request REJECTED for order {}", orderId);
        } else {
            throw new RuntimeException("Trạng thái không hợp lệ. Chỉ chấp nhận APPROVED hoặc REJECTED");
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

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

        // Đã bỏ tính tiền hoàn ở đây, chuyển sang lúc hoàn tất (RETURNED)

        order.setOrderStatus(OrderStatus.RETURN_RECEIVED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

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

    /**
     * Kiểm tra chuyển trạng thái hợp lệ theo OrderStatus Enum
     */
    private boolean isValidAdminStatusTransition(String oldStatus, String newStatus) {
        // Danh sách các chuyển tiếp hợp lệ
        String[][] validTransitions = {
                {"PENDING_PAYMENT", "CONFIRMED"},
                {"CONFIRMED", "PROCESSING"},
                {"PROCESSING", "SHIPPING"},
                {"SHIPPING", "DELIVERED"},
                {"DELIVERED", "COMPLETED"},
                {"RETURN_APPROVED", "RETURN_RECEIVED"},
                {"RETURN_RECEIVED", "RETURNED"}
        };

        for (String[] transition : validTransitions) {
            if (transition[0].equals(oldStatus) && transition[1].equals(newStatus)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra xem đơn hàng đã trừ stock chưa
     */
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

    /**
     * Gửi thông báo cho khách hàng khi cập nhật trạng thái đơn hàng
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

        if (order.getProcessedBy() != null) {
            message += "\n\nXác nhận bởi: " + order.getProcessedBy()
                    + " (Email: " + (order.getProcessedByEmail() != null ? order.getProcessedByEmail() : "N/A") + ")";
        }

        // ✅ Thêm thông báo thanh toán nếu được cập nhật tự động
        if ("DELIVERED".equals(newStatus)) {
            message += "\n\n✅ Đơn hàng đã được giao thành công. Thanh toán đã được tự động cập nhật thành THÀNH CÔNG.";
        }

        log.info("📧 SENDING NOTIFICATION:");
        log.info("   To: {} (ID: {})", customerName, order.getCustomer() != null ? order.getCustomer().getCustomerId() : "N/A");
        log.info("   Title: {}", title);
        log.info("   Message: {}", message);
        log.info("   Order: {} ({})", orderCode, order.getOrderId());
        log.info("   Status: {} -> {}", oldStatus, newStatus);
    }

    /**
     * Lấy text hiển thị của trạng thái
     */
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

    // ================================================================
    // ===== XÂY DỰNG ĐỊA CHỈ ĐẦY ĐỦ =====
    // ================================================================

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

    // ================================================================
    // ===== CONVERT TO ADMIN RESPONSE =====
    // ================================================================

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

        // ✅ Lấy thông tin shipping address
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

            log.debug("📞 Shipping address phone for order {}: {}", order.getOrderId(), shippingPhone);
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

                log.debug("📞 Customer phone for order {}: {}", order.getOrderId(), shippingPhone);
            }
        }

        // ✅ Lấy số điện thoại hiển thị
        String displayPhone = shippingPhone;
        if (displayPhone == null || displayPhone.isEmpty()) {
            Customer customer = order.getCustomer();
            if (customer != null) {
                displayPhone = customer.getPhone();
                log.debug("📞 Using customer phone for order {}: {}", order.getOrderId(), displayPhone);
            }
        }

        log.debug("📞 Final display phone for order {}: {}", order.getOrderId(), displayPhone);

        // ✅ Lấy thông tin tiền tệ
        BigDecimal subtotal = order.getOrderSubtotalAmount() != null ? order.getOrderSubtotalAmount() : BigDecimal.ZERO;
        BigDecimal discount = order.getOrderDiscountAmount() != null ? order.getOrderDiscountAmount() : BigDecimal.ZERO;
        BigDecimal shippingFee = order.getOrderShippingFee() != null ? order.getOrderShippingFee() : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.subtract(discount).add(shippingFee);

        // ✅ Lấy thông tin voucher
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

        // ✅ Lấy thông tin khách hàng
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

        // Log để debug
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

    private ReturnRequestDetailDTO convertToReturnRequestDetailDTO(ReturnRequest returnRequest, Order order) {
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

        List<Integer> productDetailIds = null;
        if (returnRequest.getProductDetailIds() != null && !returnRequest.getProductDetailIds().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                productDetailIds = objectMapper.readValue(
                        returnRequest.getProductDetailIds(),
                        new TypeReference<List<Integer>>() {}
                );
            } catch (Exception e) {
                log.warn("Cannot parse productDetailIds: {}", e.getMessage());
            }
        }

        Map<Integer, Integer> quantities = null;
        if (returnRequest.getQuantities() != null && !returnRequest.getQuantities().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Integer> stringKeyMap = objectMapper.readValue(
                        returnRequest.getQuantities(),
                        new TypeReference<Map<String, Integer>>() {}
                );
                quantities = new java.util.HashMap<>();
                for (Map.Entry<String, Integer> entry : stringKeyMap.entrySet()) {
                    quantities.put(Integer.parseInt(entry.getKey()), entry.getValue());
                }
            } catch (Exception e) {
                log.warn("Cannot parse quantities: {}", e.getMessage());
            }
        }

        List<ReturnRequestDetailDTO.ReturnItemDetailDTO> returnItems = new java.util.ArrayList<>();
        if (order != null && productDetailIds != null && quantities != null) {
            List<OrderDetail> orderDetails = orderDetailRepository.findByOrder_OrderId(order.getOrderId());
            for (OrderDetail detail : orderDetails) {
                Integer pdId = detail.getProductDetail().getProductDetailId();
                if (productDetailIds.contains(pdId)) {
                    Integer returnQty = quantities.getOrDefault(pdId, 0);
                    if (returnQty > 0) {
                        ProductDetail pd = detail.getProductDetail();
                        String imageUrl = null;
                        if (pd.getProductImages() != null && !pd.getProductImages().isEmpty()) {
                            imageUrl = pd.getProductImages().get(0).getProductImagePath();
                        }
                        
                        BigDecimal unitFinalPrice = detail.getFinalAmount().divide(BigDecimal.valueOf(detail.getQuantity()), 2, java.math.RoundingMode.HALF_UP);
                        BigDecimal refundAmount = unitFinalPrice.multiply(BigDecimal.valueOf(returnQty));

                        ReturnRequestDetailDTO.ReturnItemDetailDTO itemDto = ReturnRequestDetailDTO.ReturnItemDetailDTO.builder()
                                .productDetailId(pdId)
                                .productName(pd.getProduct().getProductName())
                                .colorName(pd.getColor() != null ? pd.getColor().getColorName() : null)
                                .sizeName(pd.getSize() != null ? pd.getSize().getSizeName() : null)
                                .imageUrl(imageUrl)
                                .quantity(returnQty)
                                .unitPrice(unitFinalPrice)
                                .refundAmount(refundAmount)
                                .build();
                        returnItems.add(itemDto);
                    }
                }
            }
        }

        String orderCode = order != null ? "ORD" + String.format("%08d", order.getOrderId()) : null;

        return ReturnRequestDetailDTO.builder()
                .returnRequestId(returnRequest.getReturnRequestId())
                .orderId(returnRequest.getOrderId())
                .orderCode(orderCode)
                .productDetailIds(productDetailIds)
                .quantities(quantities)
                .returnItems(returnItems)
                .reason(returnRequest.getReason())
                .description(returnRequest.getDescription())
                .images(images)
                .status(returnRequest.getStatus())
                .createdAt(returnRequest.getCreatedAt())
                .updatedAt(returnRequest.getUpdatedAt())
                .adminNote(returnRequest.getAdminNote())
                .inspectionResult(returnRequest.getInspectionResult())
                .inspectionNote(returnRequest.getInspectionNote())
                .inspectedBy(returnRequest.getInspectedBy())
                .inspectedAt(returnRequest.getInspectedAt())
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

    // ================================================================
    // ===== ADMIN HỦY ĐƠN HÀNG =====
    // ================================================================

    @Override
    @Transactional
    public AdminOrderResponseDTO adminCancelOrder(Long orderId, String reason) {
        log.info("🗑️ Admin cancelling order: {}, reason: {}", orderId, reason);

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
            log.info("📦 Restoring stock for order {} before cancellation", orderId);
            List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(orderId);
            for (OrderDetail detail : details) {
                ProductDetail pd = detail.getProductDetail();
                pd.setQuantity(pd.getQuantity() + detail.getQuantity());
                productDetailRepository.save(pd);
                log.info("✅ Restored {} units for product detail: {}", detail.getQuantity(), pd.getProductDetailId());
            }
        }

        Voucher voucher = order.getVoucher();
        if (voucher != null) {
            voucher.setVoucherUsedCount(Math.max(0, voucher.getVoucherUsedCount() - 1));
            voucherRepository.save(voucher);
            log.info("🔄 Restored voucher usage: {}", voucher.getVoucherCode());
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        // ✅ Ghi nhận loại hủy để thống kê "Thống kê hủy đơn" đếm đúng (countCancelledByType LIKE %tag%)
        String existingNotes = order.getNotes();
        order.setNotes((existingNotes != null && !existingNotes.isBlank() ? existingNotes + " | " : "")
                + "Admin hủy" + (reason != null && !reason.isBlank() ? ": " + reason : ""));
        orderRepository.save(order);

        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus("CANCELLED");
        history.setNote("Admin hủy đơn hàng. Lý do: " + reason);
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Order {} cancelled by admin. Old status: {}", orderId, oldStatus);

        try {
            sendOrderStatusNotification(order, oldStatus, "CANCELLED", "Admin hủy đơn hàng. Lý do: " + reason);
        } catch (Exception e) {
            log.warn("⚠️ Không thể gửi thông báo cho khách hàng: {}", e.getMessage());
        }

        return convertToAdminResponse(order);
    }
}
