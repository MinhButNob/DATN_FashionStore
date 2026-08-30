package com.example.backend_fashionstore.quanLyBanhangTaiQuay.service;

import com.example.backend_fashionstore.auth.repository.CustomerRepository;
import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import com.example.backend_fashionstore.config.VNPayConfig;
import com.example.backend_fashionstore.enums.UserStatus;
import com.example.backend_fashionstore.exception.BusinessException;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto.*;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository.*;
import com.example.backend_fashionstore.quanLyKhachHang.dto.CustomerDTO;
import com.example.backend_fashionstore.quanLyVoucher.dto.VoucherDTO;
import com.example.backend_fashionstore.quanLyVoucher.repository.VoucherRepository;
import com.example.backend_fashionstore.quanlyNhanVien.service.EmployeeService;
import com.example.backend_fashionstore.security.SecurityUtils;
import com.example.backend_fashionstore.payment.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList; // ✅ Thêm nếu chưa có

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
    private final CashRegisterRepository cashRegisterRepository;
    private final EmployeeService employeeService;
    private final VoucherRepository voucherRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    // Tái sử dụng VNPayConfig từ luồng online — không tạo lại
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VNPayConfig vnPayConfig;
    private static final DateTimeFormatter INVOICE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private Integer getCurrentEmployeeId() {
        Integer userId = SecurityUtils.getCurrentUserId();
        return employeeService.getEmployeeIdByUserId(userId);
    }

    @Transactional
    public PosOrderResponse createOrder(PosOrderRequest request) {
        // Tự động resolve session
        PosSession session;
        Integer resolvedSessionId = request.getPosSessionId();

        if (resolvedSessionId != null) {
            session = posSessionRepository.findById(resolvedSessionId).orElse(null);
            if (session != null && !"ACTIVE".equals(session.getPosSessionStatus())) {
                throw new BusinessException("Phiên bán hàng đã đóng, vui lòng mở phiên mới");
            }
            if (session == null) {
                session = getOrCreateActiveSession();
            }
        } else {
            session = getOrCreateActiveSession();
        }
        resolvedSessionId = session.getPosSessionId();

        // ===== XỬ LÝ KHÁCH HÀNG =====
        Integer customerId = null;
        String customerName = request.getPosOrderCustomerName();
        String customerPhone = request.getPosOrderCustomerPhone();

        if (customerPhone != null && !customerPhone.trim().isEmpty()) {
            Customer existingCustomer = customerRepository.findByPhone(customerPhone.trim()).orElse(null);
            if (existingCustomer != null) {
                customerId = existingCustomer.getCustomerId();
                if (customerName != null && !customerName.trim().isEmpty()) {
                    existingCustomer.setCustomerName(customerName.trim());
                    customerRepository.save(existingCustomer);
                }
            } else {
                Customer newCustomer = createNewCustomer(customerName, customerPhone);
                customerId = newCustomer.getCustomerId();
            }
        }

        // Tính toán
        BigDecimal subtotal = calculateSubtotal(request.getItems());

        // ===== XỬ LÝ VOUCHER =====
        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            Voucher voucher = voucherRepository.findActiveByVoucherCode(request.getVoucherCode())
                    .orElseThrow(() -> new BusinessException("Mã giảm giá không tồn tại hoặc không còn hoạt động"));

            if (!"ACTIVE".equals(voucher.getVoucherStatus()) || voucher.getVoucherDeletedAt() != null) {
                throw new BusinessException("Mã giảm giá đang bị tạm dừng hoặc đã bị xóa");
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(voucher.getVoucherStartDate()) || now.isAfter(voucher.getVoucherEndDate())) {
                throw new BusinessException("Mã giảm giá đã hết hạn hoặc chưa đến ngày sử dụng");
            }
            if (voucher.getVoucherMaxUsage() != null && voucher.getVoucherUsedCount() >= voucher.getVoucherMaxUsage()) {
                throw new BusinessException("Mã giảm giá đã hết lượt sử dụng");
            }
            if (voucher.getVoucherMinOrderValue() != null && subtotal.compareTo(voucher.getVoucherMinOrderValue()) < 0) {
                throw new BusinessException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã giảm giá này");
            }

            // Tính giảm giá
            if (voucher.getVoucherDiscountPercent() != null && voucher.getVoucherDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                discountAmount = subtotal.multiply(voucher.getVoucherDiscountPercent())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                if (voucher.getVoucherDiscountAmount() != null && voucher.getVoucherDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    if (discountAmount.compareTo(voucher.getVoucherDiscountAmount()) > 0) {
                        discountAmount = voucher.getVoucherDiscountAmount();
                    }
                }
            } else if (voucher.getVoucherDiscountAmount() != null) {
                discountAmount = voucher.getVoucherDiscountAmount();
            }

            if (discountAmount.compareTo(subtotal) > 0) {
                discountAmount = subtotal;
            }

            voucher.setVoucherUsedCount(voucher.getVoucherUsedCount() + 1);
            voucherRepository.save(voucher);
            appliedVoucher = voucher;
        } else {
            discountAmount = request.getPosOrderDiscountAmount() != null ?
                    request.getPosOrderDiscountAmount() : BigDecimal.ZERO;
        }

        BigDecimal taxAmount = request.getPosOrderTaxAmount() != null ? request.getPosOrderTaxAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.subtract(discountAmount).add(taxAmount);

        boolean hasPayments = request.getPayments() != null && !request.getPayments().isEmpty();
        BigDecimal paidAmount = hasPayments ? calculatePaidAmount(request.getPayments()) : BigDecimal.ZERO;

        if (hasPayments) {
            BigDecimal changeAmount = paidAmount.subtract(totalAmount);
            if (changeAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(
                        "Số tiền thanh toán không đủ. Thiếu: " + changeAmount.abs() + "đ"
                );
            }
        }

        // Đơn VNPay chưa thanh toán dùng cùng trạng thái với đơn online.
        // PENDING không còn được database hiện tại chấp nhận trong chk_pos_order_status.
        String orderStatus = hasPayments ? "COMPLETED" : "PENDING_PAYMENT";
        String paymentStatus = hasPayments ? "PAID" : "UNPAID";
        String invoiceNumber = generateInvoiceNumber();

        PosOrder order = PosOrder.builder()
                .posOrderSessionId(resolvedSessionId)
                .posOrderCustomerId(customerId)
                .posOrderCustomerName(customerName != null ? customerName : "Khách lẻ")
                .posOrderCustomerPhone(customerPhone)
                .posOrderInvoiceNumber(invoiceNumber)
                .posOrderType(request.getPosOrderType())
                .posOrderSubtotal(subtotal)
                .posOrderDiscountAmount(discountAmount)
                .posOrderTaxAmount(taxAmount)
                .posOrderPaidAmount(paidAmount)
                .posOrderTotalAmount(totalAmount)
                .posOrderChangeAmount(hasPayments ? paidAmount.subtract(totalAmount) : BigDecimal.ZERO)
                .posOrderStatus(orderStatus)
                .posOrderPaymentStatus(paymentStatus)
                .posOrderNotes(request.getPosOrderNotes())
                .voucherId(appliedVoucher != null ? appliedVoucher.getVoucherId() : null)
                .build();

        PosOrder savedOrder = posOrderRepository.save(order);

        // Tạo order items VÀ TRỪ TỒN KHO
        for (PosOrderItemRequest itemReq : request.getItems()) {
            Integer currentStock = productDetailRepository.getQuantityById(itemReq.getProductDetailId());
            if (currentStock == null || currentStock < itemReq.getQuantity()) {
                throw new BusinessException("Sản phẩm không đủ tồn kho. Còn: " + currentStock);
            }

            int updated = productDetailRepository.decreaseQuantity(
                    itemReq.getProductDetailId(),
                    itemReq.getQuantity()
            );
            if (updated == 0) {
                throw new BusinessException("Cập nhật tồn kho thất bại, vui lòng thử lại");
            }

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
            Integer paymentMethodId = resolvePosPaymentMethodId(paymentReq.getPaymentMethodId());
            PosPayment payment = PosPayment.builder()
                    .posPaymentOrderId(savedOrder.getPosOrderId())
                    .posPaymentMethodId(paymentMethodId)
                    .posPaymentAmount(paymentReq.getAmount())
                    .posPaymentReferenceNumber(paymentReq.getReferenceNumber())
                    .posPaymentStatus("COMPLETED")
                    .posPaymentProcessedAt(LocalDateTime.now())
                    .build();
            posPaymentRepository.save(payment);
        }

        if (session != null && hasPayments) {
            updateSessionSales(session, totalAmount, request.getPayments());
        }

        return convertToResponse(savedOrder);
    }

    private Customer createNewCustomer(String customerName, String phone) {
        User user = User.builder()
                .username("customer_" + System.currentTimeMillis())
                .email("customer_" + System.currentTimeMillis() + "@temp.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        Customer customer = Customer.builder()
                .user(user)
                .customerName(customerName != null ? customerName : "Khách lẻ")
                .phone(phone)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return customerRepository.save(customer);
    }

    public List<PosOrderResponse> getOrdersBySession(Integer sessionId) {
        return posOrderRepository.findByPosOrderSessionId(sessionId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public PosOrderResponse getOrderByInvoiceNumber(String invoiceNumber) {
        return posOrderRepository.findByPosOrderInvoiceNumber(invoiceNumber)
                .map(this::convertToResponse)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn hàng với mã: " + invoiceNumber));
    }

    public PosOrderResponse getOrderById(Integer orderId) {
        return posOrderRepository.findById(orderId)
                .map(this::convertToResponse)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn hàng với ID: " + orderId));
    }

    public List<PosOrderResponse> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return posOrderRepository.findOrdersBetweenDates(startDate, endDate)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

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

        BigDecimal orderTotal = order.getPosOrderSubtotal()
                .subtract(order.getPosOrderDiscountAmount())
                .add(order.getPosOrderTaxAmount());

        if (request.getAmount().compareTo(orderTotal) > 0) {
            throw new BusinessException("Số tiền hoàn không được lớn hơn tổng đơn hàng");
        }

        List<PosOrderItem> items = posOrderItemRepository.findByPosOrderItemOrderId(order.getPosOrderId());

        for (PosOrderItem item : items) {
            productDetailRepository.increaseQuantity(
                    item.getPosOrderItemProductDetailId(),
                    item.getPosOrderItemQuantity()
            );
        }

        Integer currentEmployeeId = getCurrentEmployeeId();

        PosRefund refund = PosRefund.builder()
                .posRefundOrderId(request.getOrderId())
                .posRefundAmount(request.getAmount())
                .posRefundReason(request.getReason())
                .posRefundMethodId(request.getRefundMethodId())
                .posRefundEmployeeId(currentEmployeeId)
                .posRefundStatus("COMPLETED")
                .posRefundApprovedAt(LocalDateTime.now())
                .build();

        order.setPosOrderStatus("REFUNDED");
        order.setPosOrderPaymentStatus("REFUNDED");
        posOrderRepository.save(order);

        updateSessionOnRefund(order, request.getAmount());

        PosRefund savedRefund = posRefundRepository.save(refund);

        return convertRefundToResponse(savedRefund);
    }

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

    public List<PosReceiptLog> getReceiptLogsByOrder(Integer orderId) {
        return posReceiptLogRepository.findByReceiptLogOrderId(orderId);
    }

    // PosService.java - Thay thế method này
    public PosOrderDetailResponse getOrderDetailWithProducts(Integer orderId) {
        PosOrder order = posOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn hàng"));

        // ✅ Log để debug
        log.info("📦 Getting order detail for orderId: {}", orderId);

        // ✅ Lấy danh sách items
        List<PosOrderItem> items = posOrderItemRepository.findByPosOrderItemOrderId(orderId);

        // ✅ Log số lượng items
        log.info("📦 Found {} items for order: {}", items.size(), orderId);

        // ✅ Tính tổng giảm giá và subtotal
        BigDecimal totalDiscount = order.getPosOrderDiscountAmount() != null ?
                order.getPosOrderDiscountAmount() : BigDecimal.ZERO;
        BigDecimal subtotal = order.getPosOrderSubtotal() != null ?
                order.getPosOrderSubtotal() : BigDecimal.ZERO;

        List<PosOrderItemDetailResponse> itemDetails = new ArrayList<>();

        for (PosOrderItem item : items) {
            ProductDetail productDetail = productDetailRepository.findByIdWithRelations(item.getPosOrderItemProductDetailId())
                    .orElse(null);

            PosOrderItemDetailResponse detail = new PosOrderItemDetailResponse();
            detail.setPosOrderItemId(item.getPosOrderItemId());
            detail.setQuantity(item.getPosOrderItemQuantity());
            detail.setUnitPrice(item.getPosOrderItemUnitPrice());
            detail.setDiscount(item.getPosOrderItemDiscount());
            detail.setTotal(item.getPosOrderItemTotal());

            // ✅ Lấy thông tin sản phẩm
            if (productDetail != null) {
                detail.setProductName(productDetail.getProduct() != null ?
                        productDetail.getProduct().getProductName() : "Sản phẩm");
                detail.setColorName(productDetail.getColor() != null ?
                        productDetail.getColor().getColorName() : "N/A");
                detail.setSizeName(productDetail.getSize() != null ?
                        productDetail.getSize().getSizeName() : "N/A");
                detail.setSku(productDetail.getSku());
            } else {
                // ✅ Fallback nếu không tìm thấy ProductDetail
                detail.setProductName("Sản phẩm #" + item.getPosOrderItemProductDetailId());
                detail.setColorName("N/A");
                detail.setSizeName("N/A");
            }

            // ✅ Tính discountPerUnit và finalUnitPrice
            BigDecimal itemTotal = item.getPosOrderItemUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getPosOrderItemQuantity()));

            BigDecimal discountPerUnit = BigDecimal.ZERO;
            if (totalDiscount.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(BigDecimal.ZERO) > 0) {
                // Tính tỷ lệ giảm giá theo item
                BigDecimal ratio = itemTotal.divide(subtotal, 10, RoundingMode.HALF_UP);
                BigDecimal itemDiscount = totalDiscount.multiply(ratio)
                        .divide(BigDecimal.valueOf(item.getPosOrderItemQuantity()), 0, RoundingMode.HALF_UP);
                discountPerUnit = itemDiscount;
            }

            // Nếu có giảm giá từng sản phẩm riêng, cộng thêm
            if (item.getPosOrderItemDiscount() != null) {
                discountPerUnit = discountPerUnit.add(item.getPosOrderItemDiscount());
            }

            BigDecimal finalUnitPrice = item.getPosOrderItemUnitPrice().subtract(discountPerUnit);
            if (finalUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
                finalUnitPrice = BigDecimal.ZERO;
            }

            detail.setDiscountPerUnit(discountPerUnit);
            detail.setFinalUnitPrice(finalUnitPrice);

            // ✅ Log chi tiết từng item
            log.info("📦 Item: productName={}, quantity={}, unitPrice={}, discountPerUnit={}, finalUnitPrice={}",
                    detail.getProductName(), detail.getQuantity(), detail.getUnitPrice(),
                    discountPerUnit, finalUnitPrice);

            itemDetails.add(detail);
        }

        PosOrderDetailResponse response = new PosOrderDetailResponse();
        response.setOrder(convertToResponse(order));
        response.setItems(itemDetails);

        log.info("✅ Returning {} items for order: {}", itemDetails.size(), orderId);

        return response;
    }

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

    @Transactional
    public PosPaymentResponse createVnpayPaymentForPos(Integer orderId, String returnUrl, String clientIp) {
        PosOrder order = posOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn hàng POS: " + orderId));

        if ("PAID".equals(order.getPosOrderPaymentStatus()) || "REFUNDED".equals(order.getPosOrderPaymentStatus())) {
            throw new BusinessException("Đơn hàng đã được thanh toán hoặc đã hoàn tiền, không thể tạo link VNPay");
        }

        String txnRef = "POS-" + order.getPosOrderInvoiceNumber() + "-" + VNPayConfig.getRandomNumber(4);

        BigDecimal orderTotal = order.getPosOrderSubtotal()
                .subtract(order.getPosOrderDiscountAmount())
                .add(order.getPosOrderTaxAmount());
        BigDecimal vnpayAmount = orderTotal.compareTo(BigDecimal.ZERO) > 0 ? orderTotal : BigDecimal.ZERO;

        if (vnpayAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Tổng đơn hàng không hợp lệ để tạo link VNPay");
        }

        long amountVnd = vnpayAmount.longValue() * 100;

        String effectiveReturnUrl = (returnUrl != null && !returnUrl.isBlank())
                ? returnUrl
                : vnPayConfig.getVnp_ReturnUrl();

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVnp_Version());
        vnpParams.put("vnp_Command", vnPayConfig.getVnp_Command());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amountVnd));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "POS thanh toan tai quay:" + txnRef);
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", effectiveReturnUrl);
        vnpParams.put("vnp_IpAddr", clientIp != null ? clientIp : "127.0.0.1");

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnpParams.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnpParams.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        boolean firstField = true;
        for (String fieldName : fieldNames) {
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue == null || fieldValue.isEmpty()) {
                continue;
            }
            try {
                if (!firstField) {
                    query.append('&');
                    hashData.append('&');
                }
                String encodedName = URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString());
                String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString());
                hashData.append(encodedName).append('=').append(encodedValue);
                query.append(encodedName).append('=').append(encodedValue);
                firstField = false;
            } catch (Exception e) {
                throw new IllegalStateException("Không thể tạo dữ liệu ký VNPay", e);
            }
        }
        String secureHash = VNPayConfig.hmacSHA512(vnPayConfig.getSecretKey().trim(), hashData.toString());
        String paymentUrl = vnPayConfig.getVnp_PayUrl().trim() + "?" + query + "&vnp_SecureHash=" + secureHash;

        PosPayment pending = PosPayment.builder()
                .posPaymentOrderId(orderId)
                .posPaymentMethodId(resolvePosPaymentMethodId(2)) // 2 = VNPAY (logical ID from frontend)
                .posPaymentAmount(vnpayAmount)
                .posPaymentReferenceNumber(txnRef)
                .posPaymentStatus("PENDING")
                .posPaymentProcessedAt(LocalDateTime.now())
                .build();
        PosPayment saved = posPaymentRepository.save(pending);

        PosPaymentResponse response = convertPaymentToResponse(saved);
        response.setPaymentUrl(paymentUrl);
        response.setTxnRef(txnRef);
        return response;
    }

    @Transactional
    public PosPaymentResponse confirmVnpayForPos(Integer orderId, Map<String, String> params) {
        PosOrder order = posOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn hàng POS: " + orderId));

        log.info("--- [DEBUG VNPAY POS] Start confirmVnpayForPos ---");
        log.info("orderId: {}", orderId);
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        log.info("txnRef: {}, responseCode: {}", txnRef, responseCode);
        log.info("paymentStatus trước update: {}", order.getPosOrderPaymentStatus());
        log.info("orderStatus trước update: {}", order.getPosOrderStatus());

        Map<String, String> fields = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("vnp_")) {
                fields.put(entry.getKey(), entry.getValue());
            }
        }
        String receivedHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        String computedHash = hashAllFieldsPos(fields);

        if (!computedHash.equalsIgnoreCase(receivedHash)) {
            throw new BusinessException("Chữ ký VNPay không hợp lệ");
        }

        PosPayment payment = posPaymentRepository.findByPosPaymentOrderId(orderId)
                .stream()
                .filter(p -> txnRef.equals(p.getPosPaymentReferenceNumber()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Không tìm thấy bản ghi thanh toán VNPay cho đơn " + orderId));

        BigDecimal callbackAmount = params.get("vnp_Amount") == null ? null
                : new BigDecimal(params.get("vnp_Amount")).divide(new BigDecimal(100));
        if (callbackAmount == null || callbackAmount.compareTo(payment.getPosPaymentAmount()) != 0) {
            throw new BusinessException("Số tiền VNPay không khớp với đơn hàng");
        }

        if ("COMPLETED".equals(payment.getPosPaymentStatus())) {
            return convertPaymentToResponse(payment);
        }

        if ("00".equals(responseCode)) {
            payment.setPosPaymentStatus("COMPLETED");
            payment.setPosPaymentProcessedAt(LocalDateTime.now());
            posPaymentRepository.save(payment);

            BigDecimal orderTotal = order.getPosOrderSubtotal()
                    .subtract(order.getPosOrderDiscountAmount())
                    .add(order.getPosOrderTaxAmount());
            BigDecimal newPaid = (order.getPosOrderPaidAmount() != null ? order.getPosOrderPaidAmount() : BigDecimal.ZERO)
                    .add(payment.getPosPaymentAmount());
            order.setPosOrderPaidAmount(newPaid);

            if (newPaid.compareTo(orderTotal) >= 0 || "00".equals(responseCode)) {
                order.setPosOrderPaymentStatus("PAID");
                order.setPosOrderStatus("COMPLETED");
            }
            posOrderRepository.save(order);

            log.info("paymentStatus sau update: {}", order.getPosOrderPaymentStatus());
            log.info("orderStatus sau update: {}", order.getPosOrderStatus());

            if (order.getPosOrderSessionId() != null) {
                posSessionRepository.findById(order.getPosOrderSessionId()).ifPresent(session -> {
                    BigDecimal transfer = session.getPosSessionTransferSales().add(payment.getPosPaymentAmount());
                    session.setPosSessionTransferSales(transfer);
                    posSessionRepository.save(session);
                });
            }
        } else {
            payment.setPosPaymentStatus("FAILED");
            payment.setPosPaymentProcessedAt(LocalDateTime.now());
            posPaymentRepository.save(payment);

            order.setPosOrderPaymentStatus("UNPAID");
            order.setPosOrderStatus("PENDING_PAYMENT");
            posOrderRepository.save(order);

            log.info("paymentStatus sau update (Thất bại): {}", order.getPosOrderPaymentStatus());
            log.info("orderStatus sau update (Thất bại): {}", order.getPosOrderStatus());
            log.info("--- [DEBUG VNPAY POS] End confirmVnpayForPos (Failed) ---");
            return convertPaymentToResponse(payment);
        }

        log.info("--- [DEBUG VNPAY POS] End confirmVnpayForPos ---");
        return convertPaymentToResponse(payment);
    }

    private String hashAllFieldsPos(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        boolean firstField = true;
        for (String name : fieldNames) {
            String val  = fields.get(name);
            if (val == null || val.isEmpty()) {
                continue;
            }
            try {
                if (!firstField) sb.append('&');
                sb.append(URLEncoder.encode(name, StandardCharsets.UTF_8.toString())).append('=')
                        .append(URLEncoder.encode(val, StandardCharsets.UTF_8.toString()));
                firstField = false;
            } catch (Exception e) {
                throw new IllegalStateException("Không thể tạo dữ liệu kiểm tra chữ ký VNPay", e);
            }
        }
        return VNPayConfig.hmacSHA512(vnPayConfig.getSecretKey().trim(), sb.toString());
    }

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

    /**
     * Frontend dùng ID logic (1=CASH, 2=VNPAY), còn ID trong database là SERIAL
     * và có thể khác giữa các môi trường. Luôn ghi ID thật theo method_name.
     */
    private Integer resolvePosPaymentMethodId(Integer logicalMethodId) {
        String methodName = Integer.valueOf(1).equals(logicalMethodId) ? "CASH" : "VNPAY";
        PaymentMethod method = paymentMethodRepository.findByMethodName(methodName)
                .orElseGet(() -> {
                    PaymentMethod created = new PaymentMethod();
                    created.setMethodName(methodName);
                    created.setDescription("CASH".equals(methodName) ? "Thanh toán tiền mặt" : "Thanh toán VNPay");
                    return paymentMethodRepository.save(created);
                });
        return Math.toIntExact(method.getPaymentMethodId());
    }

    private void updateSessionSales(PosSession session, BigDecimal amount, List<PosPaymentRequest> payments) {
        BigDecimal cashSales = session.getPosSessionCashSales();
        BigDecimal cardSales = session.getPosSessionCardSales();
        BigDecimal transferSales = session.getPosSessionTransferSales();

        for (PosPaymentRequest payment : payments) {
            Integer methodId = payment.getPaymentMethodId();
            if (methodId == 1) {
                cashSales = cashSales.add(payment.getAmount());
            } else if (methodId == 2 || methodId == 3) {
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

    // ===== CONVERT TO RESPONSE =====
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

        // Lấy thông tin voucher từ voucherId
        if (order.getVoucherId() != null) {
            voucherRepository.findById(order.getVoucherId()).ifPresent(voucher -> {
                response.setVoucherCode(voucher.getVoucherCode());
                response.setVoucherDiscountAmount(voucher.getVoucherDiscountAmount());
                response.setVoucherDiscountPercent(voucher.getVoucherDiscountPercent());
                response.setVoucherId(voucher.getVoucherId());
            });
        }

        List<PosOrderItem> items = posOrderItemRepository.findByPosOrderItemOrderId(order.getPosOrderId());
        response.setItems(items.stream().map(this::convertItemToResponse).collect(Collectors.toList()));

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

    @Transactional
    protected PosSession getOrCreateActiveSession() {
        try {
            Integer employeeId = getCurrentEmployeeId();
            if (employeeId != null) {
                PosSession mySession = posSessionRepository
                        .findActiveSessionByEmployee(employeeId)
                        .orElse(null);
                if (mySession != null) return mySession;

                Integer cashRegisterId = cashRegisterRepository.findAll()
                        .stream()
                        .filter(cr -> !"DELETED".equals(cr.getCashRegisterStatus()))
                        .findFirst()
                        .map(CashRegister::getCashRegisterId)
                        .orElse(1);

                PosSession newSession = PosSession.builder()
                        .posSessionCashRegisterId(cashRegisterId)
                        .posSessionEmployeeId(employeeId)
                        .posSessionStartTime(LocalDateTime.now())
                        .posSessionStatus("ACTIVE")
                        .posSessionStartBalance(BigDecimal.ZERO)
                        .posSessionCashSales(BigDecimal.ZERO)
                        .posSessionCardSales(BigDecimal.ZERO)
                        .posSessionTransferSales(BigDecimal.ZERO)
                        .build();

                log.info("🛒 Auto-created POS session for employeeId: {}", employeeId);
                return posSessionRepository.save(newSession);
            }
        } catch (Exception e) {
            log.warn("⚠️ Cannot resolve current employeeId for POS session: {}", e.getMessage());
        }

        List<PosSession> activeSessions = posSessionRepository.findAllActiveSessions();
        if (!activeSessions.isEmpty()) {
            return activeSessions.get(0);
        }

        Integer cashRegisterId = cashRegisterRepository.findAll()
                .stream()
                .filter(cr -> !"DELETED".equals(cr.getCashRegisterStatus()))
                .findFirst()
                .map(CashRegister::getCashRegisterId)
                .orElse(1);

        PosSession defaultSession = PosSession.builder()
                .posSessionCashRegisterId(cashRegisterId)
                .posSessionEmployeeId(1)
                .posSessionStartTime(LocalDateTime.now())
                .posSessionStatus("ACTIVE")
                .posSessionStartBalance(BigDecimal.ZERO)
                .posSessionCashSales(BigDecimal.ZERO)
                .posSessionCardSales(BigDecimal.ZERO)
                .posSessionTransferSales(BigDecimal.ZERO)
                .build();

        return posSessionRepository.save(defaultSession);
    }
    public List<PosOrderResponse> getOrdersByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException("Vui lòng nhập số điện thoại");
        }
        List<PosOrder> orders = posOrderRepository.findByPosOrderCustomerPhoneContaining(phone.trim());
        return orders.stream()
                .filter(o -> "COMPLETED".equals(o.getPosOrderStatus()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<CustomerDTO> getAllCustomers() {
        List<Customer> customers = customerRepository.findAllActiveCustomers();
        return customers.stream()
                .map(this::convertToCustomerDTO)
                .collect(Collectors.toList());
    }

    public List<CustomerDTO> searchCustomers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCustomers();
        }
        List<Customer> customers = customerRepository.searchCustomers(keyword.trim());
        return customers.stream()
                .map(this::convertToCustomerDTO)
                .collect(Collectors.toList());
    }

    private CustomerDTO convertToCustomerDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setCustomerId(customer.getCustomerId());
        dto.setCustomerName(customer.getCustomerName());
        dto.setPhone(customer.getPhone());
        dto.setUserId(customer.getUser().getUserId());
        return dto;
    }

    // ✅ Thêm method getVoucherByOrderId
    @Transactional(readOnly = true)
    public VoucherDTO getVoucherByOrderId(Integer orderId) {
        PosOrder order = posOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        Integer voucherId = order.getVoucherId();
        if (voucherId == null) {
            throw new RuntimeException("Đơn hàng không áp dụng mã giảm giá");
        }

        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));

        return convertVoucherToDTO(voucher);
    }

    // ✅ Thêm method convertVoucherToDTO
    private VoucherDTO convertVoucherToDTO(Voucher voucher) {
        VoucherDTO dto = new VoucherDTO();
        dto.setVoucherId(voucher.getVoucherId());
        dto.setVoucherCode(voucher.getVoucherCode());
        dto.setVoucherDiscountAmount(voucher.getVoucherDiscountAmount());
        dto.setVoucherDiscountPercent(voucher.getVoucherDiscountPercent());
        dto.setVoucherStartDate(voucher.getVoucherStartDate());
        dto.setVoucherEndDate(voucher.getVoucherEndDate());
        dto.setVoucherMaxUsage(voucher.getVoucherMaxUsage());
        dto.setVoucherUsedCount(voucher.getVoucherUsedCount());
        dto.setVoucherMinOrderValue(voucher.getVoucherMinOrderValue());
        dto.setVoucherStatus(voucher.getVoucherStatus());
        dto.setVoucherCreatedAt(voucher.getVoucherCreatedAt());
        dto.setVoucherUpdatedAt(voucher.getVoucherUpdatedAt());
        dto.setVoucherDeletedAt(voucher.getVoucherDeletedAt());
        return dto;
    }
}
