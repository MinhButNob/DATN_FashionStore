package com.example.backend_fashionstore.quanLyBanhangTaiQuay.service;

import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import com.example.backend_fashionstore.exception.BusinessException;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto.*;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository.*;
import com.example.backend_fashionstore.quanlyNhanVien.service.EmployeeService;
import com.example.backend_fashionstore.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PosService {

    private final PosOrderRepository posOrderRepository;
    private final PosOrderItemRepository posOrderItemRepository;
    private final PosPaymentRepository posPaymentRepository;
    private final PosSessionRepository posSessionRepository;
    private final PosRefundRepository posRefundRepository;
    private final PosReceiptLogRepository posReceiptLogRepository;
    private final ProductDetailRepository productDetailRepository;
    private final EmployeeService employeeService;
    private static final DateTimeFormatter INVOICE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private Integer getCurrentEmployeeId() {
        Integer userId = SecurityUtils.getCurrentUserId();
        return employeeService.getEmployeeIdByUserId(userId);
    }

    // =====================================================
    // TẠO ĐƠN HÀNG MỚI
    // =====================================================
    @Transactional
    public PosOrderResponse createOrder(PosOrderRequest request) {
        // Kiểm tra session đang active
        PosSession session = posSessionRepository.findById(request.getPosSessionId())
                .orElseThrow(() -> new BusinessException("Phiên bán hàng không tồn tại"));

        if (!"ACTIVE".equals(session.getPosSessionStatus())) {
            throw new BusinessException("Phiên bán hàng đã đóng, vui lòng mở phiên mới");
        }

        // Tính toán
        BigDecimal subtotal = calculateSubtotal(request.getItems());
        BigDecimal discountAmount = request.getPosOrderDiscountAmount() != null ? request.getPosOrderDiscountAmount() : BigDecimal.ZERO;
        BigDecimal taxAmount = request.getPosOrderTaxAmount() != null ? request.getPosOrderTaxAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.subtract(discountAmount).add(taxAmount);
        BigDecimal paidAmount = calculatePaidAmount(request.getPayments());
        BigDecimal changeAmount = paidAmount.subtract(totalAmount);

        if (changeAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Số tiền thanh toán không đủ. Thiếu: " + changeAmount.abs() + "đ");
        }

        // Tạo mã hóa đơn
        String invoiceNumber = generateInvoiceNumber();

        // Tạo order
        PosOrder order = PosOrder.builder()
                .posOrderSessionId(request.getPosSessionId())
                .posOrderCustomerId(request.getPosOrderCustomerId())
                .posOrderCustomerName(request.getPosOrderCustomerName())
                .posOrderCustomerPhone(request.getPosOrderCustomerPhone())
                .posOrderInvoiceNumber(invoiceNumber)
                .posOrderType(request.getPosOrderType())
                .posOrderSubtotal(subtotal)
                .posOrderDiscountAmount(discountAmount)
                .posOrderTaxAmount(taxAmount)
                .posOrderPaidAmount(paidAmount)
                .posOrderStatus("COMPLETED")
                .posOrderPaymentStatus("PAID")
                .posOrderNotes(request.getPosOrderNotes())
                .build();

        PosOrder savedOrder = posOrderRepository.save(order);

        // Tạo order items VÀ TRỪ TỒN KHO
        for (PosOrderItemRequest itemReq : request.getItems()) {
            // Kiểm tra tồn kho trước khi bán
            Integer currentStock = productDetailRepository.getQuantityById(itemReq.getProductDetailId());
            if (currentStock == null || currentStock < itemReq.getQuantity()) {
                throw new BusinessException("Sản phẩm không đủ tồn kho. Còn: " + currentStock);
            }

            // Trừ tồn kho
            int updated = productDetailRepository.decreaseQuantity(
                    itemReq.getProductDetailId(),
                    itemReq.getQuantity()
            );
            if (updated == 0) {
                throw new BusinessException("Cập nhật tồn kho thất bại, vui lòng thử lại");
            }

            // Tạo order item
            PosOrderItem item = PosOrderItem.builder()
                    .posOrderItemOrderId(savedOrder.getPosOrderId())
                    .posOrderItemProductDetailId(itemReq.getProductDetailId())
                    .posOrderItemQuantity(itemReq.getQuantity())
                    .posOrderItemUnitPrice(itemReq.getUnitPrice())
                    .posOrderItemDiscount(itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO)
                    .posOrderItemNotes(itemReq.getNotes())
                    .build();
            posOrderItemRepository.save(item);
        }

        // Tạo payments
        for (PosPaymentRequest paymentReq : request.getPayments()) {
            PosPayment payment = PosPayment.builder()
                    .posPaymentOrderId(savedOrder.getPosOrderId())
                    .posPaymentMethodId(paymentReq.getPaymentMethodId())
                    .posPaymentAmount(paymentReq.getAmount())
                    .posPaymentReferenceNumber(paymentReq.getReferenceNumber())
                    .posPaymentStatus("COMPLETED")
                    .posPaymentProcessedAt(LocalDateTime.now())
                    .build();
            posPaymentRepository.save(payment);
        }

        // Cập nhật doanh số cho session
        updateSessionSales(session, totalAmount, request.getPayments());

        return convertToResponse(savedOrder);
    }

    // =====================================================
    // LẤY ĐƠN HÀNG THEO SESSION
    // =====================================================
    public List<PosOrderResponse> getOrdersBySession(Integer sessionId) {
        return posOrderRepository.findByPosOrderSessionId(sessionId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // =====================================================
    // LẤY ĐƠN HÀNG THEO MÃ HÓA ĐƠN
    // =====================================================
    public PosOrderResponse getOrderByInvoiceNumber(String invoiceNumber) {
        return posOrderRepository.findByPosOrderInvoiceNumber(invoiceNumber)
                .map(this::convertToResponse)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn hàng với mã: " + invoiceNumber));
    }

    // =====================================================
    // LẤY ĐƠN HÀNG THEO ID
    // =====================================================
    public PosOrderResponse getOrderById(Integer orderId) {
        return posOrderRepository.findById(orderId)
                .map(this::convertToResponse)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn hàng với ID: " + orderId));
    }

    // =====================================================
    // LẤY DANH SÁCH ĐƠN HÀNG THEO KHOẢNG THỜI GIAN
    // =====================================================
    public List<PosOrderResponse> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return posOrderRepository.findOrdersBetweenDates(startDate, endDate)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // =====================================================
    // HOÀN TIỀN / TRẢ HÀNG
    // =====================================================
    @Transactional
    public PosRefundResponse refundOrder(PosRefundRequest request) {
        PosOrder order = posOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new BusinessException("Đơn hàng không tồn tại"));

        if (!"COMPLETED".equals(order.getPosOrderStatus())) {
            throw new BusinessException("Chỉ có thể hoàn tiền cho đơn hàng đã hoàn thành");
        }

        if ("REFUNDED".equals(order.getPosOrderPaymentStatus())) {
            throw new BusinessException("Đơn hàng này đã được hoàn tiền trước đó");
        }

        // Tính tổng tiền = subtotal - discount + tax (tránh phụ thuộc DB trigger)
        BigDecimal orderTotal = order.getPosOrderSubtotal()
                .subtract(order.getPosOrderDiscountAmount())
                .add(order.getPosOrderTaxAmount());

        if (request.getAmount().compareTo(orderTotal) > 0) {
            throw new BusinessException("Số tiền hoàn không được lớn hơn tổng đơn hàng");
        }

        // Lấy danh sách sản phẩm trong đơn hàng
        List<PosOrderItem> items = posOrderItemRepository.findByPosOrderItemOrderId(order.getPosOrderId());

        // CỘNG LẠI TỒN KHO cho từng sản phẩm
        for (PosOrderItem item : items) {
            productDetailRepository.increaseQuantity(
                    item.getPosOrderItemProductDetailId(),
                    item.getPosOrderItemQuantity()
            );
        }

        // Lấy employee ID từ security context
        Integer currentEmployeeId = getCurrentEmployeeId();

        // Tạo refund record
        PosRefund refund = PosRefund.builder()
                .posRefundOrderId(request.getOrderId())
                .posRefundAmount(request.getAmount())
                .posRefundReason(request.getReason())
                .posRefundMethodId(request.getRefundMethodId())
                .posRefundEmployeeId(currentEmployeeId)
                .posRefundStatus("COMPLETED")
                .posRefundApprovedAt(LocalDateTime.now())
                .build();

        // Cập nhật trạng thái order
        order.setPosOrderStatus("REFUNDED");
        order.setPosOrderPaymentStatus("REFUNDED");
        posOrderRepository.save(order);

        // Cập nhật lại doanh số của session (trừ đi)
        updateSessionOnRefund(order, request.getAmount());

        PosRefund savedRefund = posRefundRepository.save(refund);

        return convertRefundToResponse(savedRefund);
    }

    // =====================================================
    // IN HÓA ĐƠN
    // =====================================================
    @Transactional
    public PosReceiptLog printReceipt(Integer orderId, Integer copies) {
        posOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Đơn hàng không tồn tại"));

        if (copies == null || copies < 1) copies = 1;

        Integer employeeId = getCurrentEmployeeId();

        PosReceiptLog log = PosReceiptLog.builder()
                .receiptLogOrderId(orderId)
                .receiptLogPrintedAt(LocalDateTime.now())
                .receiptLogPrintedBy(employeeId)
                .receiptLogCopies(copies)
                .receiptLogStatus("SUCCESS")
                .build();

        return posReceiptLogRepository.save(log);
    }

    @Transactional
    public PosReceiptLogResponse printReceiptAsResponse(Integer orderId, Integer copies) {
        PosReceiptLog log = printReceipt(orderId, copies);
        PosReceiptLogResponse response = new PosReceiptLogResponse();
        response.setReceiptLogId(log.getReceiptLogId());
        response.setReceiptLogOrderId(log.getReceiptLogOrderId());
        response.setReceiptLogPrintedAt(log.getReceiptLogPrintedAt());
        response.setReceiptLogPrintedBy(log.getReceiptLogPrintedBy());
        response.setReceiptLogCopies(log.getReceiptLogCopies());
        response.setReceiptLogPrinterName(log.getReceiptLogPrinterName());
        response.setReceiptLogStatus(log.getReceiptLogStatus());
        response.setReceiptLogErrorMessage(log.getReceiptLogErrorMessage());
        return response;
    }

    // =====================================================
    // LẤY LỊCH SỬ IN HÓA ĐƠN
    // =====================================================
    public List<PosReceiptLog> getReceiptLogsByOrder(Integer orderId) {
        return posReceiptLogRepository.findByReceiptLogOrderId(orderId);
    }

    // =====================================================
    // LẤY CHI TIẾT ĐƠN HÀNG KÈM THÔNG TIN SẢN PHẨM
    // =====================================================
    public PosOrderDetailResponse getOrderDetailWithProducts(Integer orderId) {
        PosOrder order = posOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn hàng"));

        List<PosOrderItem> items = posOrderItemRepository.findByPosOrderItemOrderId(orderId);

        List<PosOrderItemDetailResponse> itemDetails = items.stream().map(item -> {
            ProductDetail productDetail = productDetailRepository.findByIdWithRelations(item.getPosOrderItemProductDetailId())
                    .orElse(null);

            PosOrderItemDetailResponse detail = new PosOrderItemDetailResponse();
            detail.setPosOrderItemId(item.getPosOrderItemId());
            detail.setQuantity(item.getPosOrderItemQuantity());
            detail.setUnitPrice(item.getPosOrderItemUnitPrice());
            detail.setDiscount(item.getPosOrderItemDiscount());
            detail.setTotal(item.getPosOrderItemTotal());

            if (productDetail != null) {
                detail.setProductName(productDetail.getProduct() != null ? productDetail.getProduct().getProductName() : null);
                detail.setColorName(productDetail.getColor() != null ? productDetail.getColor().getColorName() : null);
                detail.setSizeName(productDetail.getSize() != null ? productDetail.getSize().getSizeName() : null);
                detail.setSku(productDetail.getSku());
            }

            return detail;
        }).collect(Collectors.toList());

        PosOrderDetailResponse response = new PosOrderDetailResponse();
        response.setOrder(convertToResponse(order));
        response.setItems(itemDetails);

        return response;
    }

    // =====================================================
    // THỐNG KÊ NHANH THEO SESSION
    // =====================================================
    public PosSessionStatsResponse getSessionStats(Integer sessionId) {
        PosSession session = posSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("Phiên bán hàng không tồn tại"));

        Integer totalOrders = posOrderRepository.getOrderCountBySession(sessionId);
        BigDecimal totalRevenue = posOrderRepository.getTotalSalesBySession(sessionId);
        if (totalOrders == null) totalOrders = 0;
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        PosSessionStatsResponse stats = new PosSessionStatsResponse();
        stats.setSessionId(sessionId);
        stats.setCashRegisterId(session.getPosSessionCashRegisterId());
        stats.setEmployeeId(session.getPosSessionEmployeeId());
        stats.setStartTime(session.getPosSessionStartTime());
        stats.setStartBalance(session.getPosSessionStartBalance());
        stats.setTotalOrders(totalOrders);
        stats.setTotalRevenue(totalRevenue);
        stats.setAvgOrderValue(avgOrderValue);
        stats.setCashSales(session.getPosSessionCashSales());
        stats.setCardSales(session.getPosSessionCardSales());
        stats.setTransferSales(session.getPosSessionTransferSales());
        stats.setTotalSales(session.getPosSessionTotalSales());

        return stats;
    }

    // =====================================================
    // PRIVATE METHODS
    // =====================================================

    private String generateInvoiceNumber() {
        String datePart = LocalDateTime.now().format(INVOICE_FORMATTER);
        long count = posOrderRepository.count() + 1;
        return "POS-" + datePart + "-" + String.format("%06d", count);
    }

    private BigDecimal calculateSubtotal(List<PosOrderItemRequest> items) {
        return items.stream()
                .map(item -> {
                    BigDecimal discount = item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO;
                    return item.getUnitPrice()
                            .subtract(discount)
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculatePaidAmount(List<PosPaymentRequest> payments) {
        return payments.stream()
                .map(PosPaymentRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void updateSessionSales(PosSession session, BigDecimal amount, List<PosPaymentRequest> payments) {
        BigDecimal cashSales = session.getPosSessionCashSales();
        BigDecimal cardSales = session.getPosSessionCardSales();
        BigDecimal transferSales = session.getPosSessionTransferSales();

        for (PosPaymentRequest payment : payments) {
            Integer methodId = payment.getPaymentMethodId();
            if (methodId == 1) { // Tiền mặt
                cashSales = cashSales.add(payment.getAmount());
            } else if (methodId == 2 || methodId == 3) { // VNPAY / MOMO
                transferSales = transferSales.add(payment.getAmount());
            } else {
                cardSales = cardSales.add(payment.getAmount());
            }
        }

        session.setPosSessionCashSales(cashSales);
        session.setPosSessionCardSales(cardSales);
        session.setPosSessionTransferSales(transferSales);
        posSessionRepository.save(session);
    }

    private void updateSessionOnRefund(PosOrder order, BigDecimal refundAmount) {
        PosSession session = posSessionRepository.findById(order.getPosOrderSessionId()).orElse(null);
        if (session != null) {
            BigDecimal newCashSales = session.getPosSessionCashSales().subtract(refundAmount);
            if (newCashSales.compareTo(BigDecimal.ZERO) < 0) {
                newCashSales = BigDecimal.ZERO;
            }
            session.setPosSessionCashSales(newCashSales);
            posSessionRepository.save(session);
        }
    }

    private PosOrderResponse convertToResponse(PosOrder order) {
        PosOrderResponse response = new PosOrderResponse();
        response.setPosOrderId(order.getPosOrderId());
        response.setPosOrderSessionId(order.getPosOrderSessionId());
        response.setPosOrderCustomerId(order.getPosOrderCustomerId());
        response.setPosOrderInvoiceNumber(order.getPosOrderInvoiceNumber());
        response.setPosOrderType(order.getPosOrderType());
        response.setPosOrderSubtotal(order.getPosOrderSubtotal());
        response.setPosOrderDiscountAmount(order.getPosOrderDiscountAmount());
        response.setPosOrderTaxAmount(order.getPosOrderTaxAmount());
        response.setPosOrderTotalAmount(order.getPosOrderTotalAmount());
        response.setPosOrderPaidAmount(order.getPosOrderPaidAmount());
        response.setPosOrderChangeAmount(order.getPosOrderChangeAmount());
        response.setPosOrderStatus(order.getPosOrderStatus());
        response.setPosOrderPaymentStatus(order.getPosOrderPaymentStatus());
        response.setPosOrderCustomerName(order.getPosOrderCustomerName());
        response.setPosOrderCustomerPhone(order.getPosOrderCustomerPhone());
        response.setPosOrderNotes(order.getPosOrderNotes());
        response.setPosOrderCreatedAt(order.getPosOrderCreatedAt());

        // Lấy items
        List<PosOrderItem> items = posOrderItemRepository.findByPosOrderItemOrderId(order.getPosOrderId());
        response.setItems(items.stream().map(this::convertItemToResponse).collect(Collectors.toList()));

        // Lấy payments
        List<PosPayment> payments = posPaymentRepository.findByPosPaymentOrderId(order.getPosOrderId());
        response.setPayments(payments.stream().map(this::convertPaymentToResponse).collect(Collectors.toList()));

        return response;
    }

    private PosOrderItemResponse convertItemToResponse(PosOrderItem item) {
        PosOrderItemResponse response = new PosOrderItemResponse();
        response.setPosOrderItemId(item.getPosOrderItemId());
        response.setProductDetailId(item.getPosOrderItemProductDetailId());
        response.setQuantity(item.getPosOrderItemQuantity());
        response.setUnitPrice(item.getPosOrderItemUnitPrice());
        response.setDiscount(item.getPosOrderItemDiscount());
        response.setTotal(item.getPosOrderItemTotal());
        response.setNotes(item.getPosOrderItemNotes());
        return response;
    }

    private PosPaymentResponse convertPaymentToResponse(PosPayment payment) {
        PosPaymentResponse response = new PosPaymentResponse();
        response.setPosPaymentId(payment.getPosPaymentId());
        response.setPaymentMethodId(payment.getPosPaymentMethodId());
        response.setAmount(payment.getPosPaymentAmount());
        response.setReferenceNumber(payment.getPosPaymentReferenceNumber());
        response.setStatus(payment.getPosPaymentStatus());
        response.setProcessedAt(payment.getPosPaymentProcessedAt());
        return response;
    }

    private PosRefundResponse convertRefundToResponse(PosRefund refund) {
        PosRefundResponse response = new PosRefundResponse();
        response.setPosRefundId(refund.getPosRefundId());
        response.setPosRefundOrderId(refund.getPosRefundOrderId());
        response.setPosRefundAmount(refund.getPosRefundAmount());
        response.setPosRefundReason(refund.getPosRefundReason());
        response.setPosRefundMethodId(refund.getPosRefundMethodId());
        response.setPosRefundEmployeeId(refund.getPosRefundEmployeeId());
        response.setPosRefundStatus(refund.getPosRefundStatus());
        response.setPosRefundApprovedAt(refund.getPosRefundApprovedAt());
        response.setPosRefundCreatedAt(refund.getPosRefundCreatedAt());
        return response;
    }
}