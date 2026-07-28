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
public class SaleStaffService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ProductDetailRepository productDetailRepository;
    private final CustomerOrderServiceImpl customerOrderService;
    private final VoucherRepository voucherRepository;
    private final KhachHangCustomerRepository customerRepository;

    // =============================================
    // QUẢN LÝ ĐƠN HÀNG (FULL CHỨC NĂNG)
    // =============================================

    /**
     * Lấy danh sách đơn hàng
     */
    public Page<AdminOrderResponseDTO> getAllOrders(String status, String keyword, Pageable pageable) {
        Page<Order> orders;

        log.info("getAllOrders - status: {}, keyword: {}, pageable: {}", status, keyword, pageable);

        // Xử lý trường hợp filter REJECTED
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

    /**
     * Lấy chi tiết đơn hàng
     */
    public AdminOrderResponseDTO getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
        return convertToAdminResponse(order);
    }

    /**
     * Cập nhật trạng thái đơn hàng
     */
    public AdminOrderResponseDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        String oldStatus = order.getOrderStatus().toString();
        String newStatus = request.getOrderStatus();

        // Kiểm tra chuyển trạng thái hợp lệ
        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new RuntimeException("Không thể chuyển trạng thái từ '" + oldStatus + "' sang '" + newStatus + "'");
        }

        // TRỪ KHO KHI CHUYỂN TỪ PROCESSING -> SHIPPING
        if ("PROCESSING".equals(oldStatus) && "SHIPPING".equals(newStatus)) {
            try {
                customerOrderService.deductStockForOrder(orderId);
            } catch (Exception e) {
                log.error("❌ Failed to deduct stock for order {}: {}", orderId, e.getMessage());
                throw new RuntimeException("Không thể trừ số lượng sản phẩm trong kho: " + e.getMessage());
            }
        }

        // Cập nhật trạng thái
        order.setOrderStatus(OrderStatus.valueOf(newStatus));
        order.setUpdatedAt(LocalDateTime.now());

        // Nếu chuyển từ RETURN_RECEIVED sang RETURNED, cập nhật trạng thái return request
        if ("RETURN_RECEIVED".equals(oldStatus) && "RETURNED".equals(newStatus)) {
            returnRequestRepository.findFirstByOrderIdAndDeletedAtIsNull(orderId)
                    .ifPresent(returnRequest -> {
                        returnRequest.setStatus("COMPLETED");
                        returnRequest.setUpdatedAt(LocalDateTime.now());
                        returnRequestRepository.save(returnRequest);
                        log.info("✅ Return request completed for order {}", orderId);
                    });
        }

        // Nếu chuyển từ SHIPPING -> CANCELLED, hoàn lại kho
        if ("CANCELLED".equals(newStatus) && isStockDeducted(oldStatus)) {
            restoreStockForOrder(orderId);
        }

        orderRepository.save(order);

        // Ghi lịch sử
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus(newStatus);
        history.setNote("Cập nhật trạng thái từ '" + getStatusText(oldStatus) + "' thành '" + getStatusText(newStatus) + "'. " +
                (request.getNotes() != null ? request.getNotes() : ""));
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Updated order {} status from {} to {}", orderId, oldStatus, newStatus);

        return convertToAdminResponse(order);
    }

    /**
     * Cập nhật trạng thái thanh toán
     */
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

    /**
     * Admin hủy đơn hàng
     */
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

        // Nếu đơn hàng đã trừ kho, hoàn lại kho
        if (isStockDeducted(oldStatus)) {
            restoreStockForOrder(orderId);
        }

        // Nếu có voucher, hoàn lại số lần sử dụng
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

    /**
     * Thống kê đơn hàng
     */
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

        // 🆕 THỐNG KÊ HOÀN HÀNG CHI TIẾT
        long returnRequested = orderRepository.countByOrderStatus(OrderStatus.RETURN_REQUESTED);
        long returnApproved = orderRepository.countByOrderStatus(OrderStatus.RETURN_APPROVED);
        long returnReceived = orderRepository.countByOrderStatus(OrderStatus.RETURN_RECEIVED);
        long returnPending = orderRepository.countByOrderStatus(OrderStatus.RETURN_PENDING);
        long returnCompleted = orderRepository.countByOrderStatus(OrderStatus.RETURNED);
        long returnRejected = orderRepository.countByOrderStatus(OrderStatus.REJECTED);

        // 🆕 THỐNG KÊ DOANH THU
        BigDecimal todayRevenue = orderRepository.getTodayRevenue();
        if (todayRevenue == null) todayRevenue = BigDecimal.ZERO;

        BigDecimal monthlyRevenue = orderRepository.getMonthlyRevenue(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue()
        );
        if (monthlyRevenue == null) monthlyRevenue = BigDecimal.ZERO;

        // Tổng doanh thu từ đơn hàng COMPLETED
        BigDecimal totalRevenue = orderRepository.getTotalRevenue();
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        // Doanh thu online (không phải POS)
        BigDecimal onlineRevenue = orderRepository.getOnlineRevenue();
        if (onlineRevenue == null) onlineRevenue = BigDecimal.ZERO;

        // Tổng tiền hoàn trả
        BigDecimal returnRefundAmount = orderRepository.getReturnRefundAmount();
        if (returnRefundAmount == null) returnRefundAmount = BigDecimal.ZERO;

        // Doanh thu thực tế
        BigDecimal netRevenue = onlineRevenue.subtract(returnRefundAmount);
        if (netRevenue.compareTo(BigDecimal.ZERO) < 0) netRevenue = BigDecimal.ZERO;

        // Thống kê hủy đơn chi tiết
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

    /**
     * Đếm theo trạng thái
     */
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

    /**
     * Lấy chi tiết yêu cầu hoàn hàng
     */
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

    /**
     * Xử lý yêu cầu hoàn hàng
     */
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

    /**
     * Xác nhận đã nhận hàng trả
     */
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

    /**
     * Lấy tất cả yêu cầu hoàn hàng
     */
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

    /**
     * Lấy hóa đơn
     */
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
                .shippingFullAddress(shippingFullAddress) // ✅ THÊM shippingFullAddress
                .voucherCode(voucherCode)
                .voucherDiscountAmount(voucherDiscountAmount)
                .items(items)
                .build();
    }

    // =============================================
    // QUẢN LÝ SẢN PHẨM (CHỈ CẬP NHẬT SỐ LƯỢNG)
    // =============================================

    /**
     * Cập nhật số lượng sản phẩm trong kho
     */
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

    /**
     * Tăng số lượng sản phẩm trong kho
     */
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

    /**
     * Giảm số lượng sản phẩm trong kho
     */
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

    /**
     * Lấy chi tiết biến thể sản phẩm
     */
    public ProductDetail getProductDetail(Integer productDetailId) {
        return productDetailRepository.findByIdWithRelations(productDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm"));
    }

    // =============================================
    // QUẢN LÝ KHÁCH HÀNG (CHỈ XEM)
    // =============================================

    /**
     * Lấy danh sách khách hàng
     */
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

    /**
     * Lấy chi tiết khách hàng
     */
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
        // Không thể chuyển sang CANCELLED (do khách hàng hủy)
        if ("CANCELLED".equals(newStatus)) {
            return false;
        }

        // Chuyển từ RETURN_APPROVED -> RETURN_RECEIVED
        if ("RETURN_APPROVED".equals(oldStatus) && "RETURN_RECEIVED".equals(newStatus)) {
            return true;
        }

        // Chuyển từ RETURN_RECEIVED -> RETURNED
        if ("RETURN_RECEIVED".equals(oldStatus) && "RETURNED".equals(newStatus)) {
            return true;
        }

        // Chuyển từ PROCESSING -> SHIPPING
        if ("PROCESSING".equals(oldStatus) && "SHIPPING".equals(newStatus)) {
            return true;
        }

        // Chuyển theo thứ tự: PENDING_PAYMENT -> CONFIRMED -> PROCESSING -> SHIPPING -> DELIVERED -> COMPLETED
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

    // =============================================
    // XÂY DỰNG ĐỊA CHỈ ĐẦY ĐỦ
    // =============================================

    /**
     * ✅ HÀM XÂY DỰNG ĐỊA CHỈ ĐẦY ĐỦ
     */
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

        // ✅ LẤY SHIPPING ADDRESS TỪ ORDER
        String shippingAddressStr = order.getShippingAddress();
        AdminShippingAddressDTO shippingDTO = null;

        if (shippingAddressStr != null && !shippingAddressStr.isEmpty()) {
            Customer customer = order.getCustomer();
            String shippingFullName = "";
            String shippingPhone = "";
            if (customer != null) {
                shippingFullName = customer.getCustomerName() != null ? customer.getCustomerName() : "";
                shippingPhone = customer.getPhone() != null ? customer.getPhone() : "";
            }

            shippingDTO = AdminShippingAddressDTO.builder()
                    .addressId(null)
                    .fullName(shippingFullName)
                    .phone(shippingPhone)
                    .addressLine(shippingAddressStr)
                    .city("")
                    .district("")
                    .ward("")
                    .fullAddress(shippingAddressStr) // ✅ ĐỊA CHỈ ĐẦY ĐỦ
                    .build();
        } else {
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
                        .fullAddress(customer.getAddress())
                        .build();
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
                .shippingAddress(shippingDTO)
                .voucher(voucherDTO)
                .items(itemResponses)
                .build();
    }
}