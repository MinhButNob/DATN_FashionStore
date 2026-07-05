package com.example.backend_fashionstore.customer.order.service;

import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderDetailRepository;
import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderRepository;
import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import com.example.backend_fashionstore.auth.repository.CustomerRepository;
import com.example.backend_fashionstore.customer.cart.repository.CartItemRepository;
import com.example.backend_fashionstore.customer.cart.repository.CartRepository;
import com.example.backend_fashionstore.customer.order.dto.request.OrderRequestDTO;
import com.example.backend_fashionstore.customer.order.dto.request.OrderItemRequestDTO;
import com.example.backend_fashionstore.customer.order.dto.request.ReturnRequestDTO;
import com.example.backend_fashionstore.customer.order.dto.response.OrderResponseDTO;
import com.example.backend_fashionstore.customer.order.dto.response.CustomerInfoDTO;
import com.example.backend_fashionstore.customer.order.dto.response.ShippingAddressDTO;
import com.example.backend_fashionstore.customer.order.dto.response.VoucherInfoDTO;
import com.example.backend_fashionstore.customer.order.dto.response.OrderItemResponseDTO;
import com.example.backend_fashionstore.customer.order.dto.response.ReturnRequestResponseDTO;
import com.example.backend_fashionstore.customer.order.repository.OrderHistoryRepository;
import com.example.backend_fashionstore.customer.order.repository.ShippingAddressRepository;
import com.example.backend_fashionstore.customer.order.repository.ReturnRequestRepository;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.enums.OrderStatus;
import com.example.backend_fashionstore.enums.PaymentStatus;
import com.example.backend_fashionstore.quanLyVoucher.repository.VoucherRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerOrderServiceImpl implements CustomerOrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final CustomerRepository customerRepository;
    private final ProductDetailRepository productDetailRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    private final VoucherRepository voucherRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ReturnRequestRepository returnRequestRepository;

    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("30000");

    // ============================================
    // 1. TẠO ĐƠN HÀNG
    // ============================================
    @Override
    @Transactional
    public OrderResponseDTO createOrder(Integer customerId, OrderRequestDTO request) {
        log.info("Creating order for customer: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        List<OrderItemRequestDTO> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Không có sản phẩm trong đơn hàng");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderDetail> orderDetails = new ArrayList<>();

        for (OrderItemRequestDTO itemReq : items) {
            ProductDetail productDetail = productDetailRepository.findById(itemReq.getProductDetailId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm: " + itemReq.getProductDetailId()));

            if (productDetail.getQuantity() < itemReq.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + productDetail.getProduct().getProductName() +
                        " không đủ số lượng. Còn: " + productDetail.getQuantity());
            }

            BigDecimal unitPrice = productDetail.getPrice();
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(itemTotal);

            OrderDetail detail = new OrderDetail();
            detail.setProductDetail(productDetail);
            detail.setQuantity(itemReq.getQuantity());
            detail.setUnitPrice(unitPrice);
            detail.setDiscount(BigDecimal.ZERO);
            detail.setFinalAmount(itemTotal);
            orderDetails.add(detail);
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;

        if (request.getVoucherId() != null) {
            log.info("🔍 Applying voucher with ID: {}", request.getVoucherId());

            Voucher voucher = voucherRepository.findById(request.getVoucherId())
                    .orElse(null);

            if (voucher != null && isValidVoucher(voucher)) {
                if (voucher.getVoucherMinOrderValue() != null &&
                        subtotal.compareTo(voucher.getVoucherMinOrderValue()) < 0) {
                    log.warn("⚠️ Order value {} is less than min order value {}",
                            subtotal, voucher.getVoucherMinOrderValue());
                    throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher: " +
                            voucher.getVoucherMinOrderValue());
                }

                if (voucher.getVoucherDiscountAmount() != null &&
                        voucher.getVoucherDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    discountAmount = voucher.getVoucherDiscountAmount().min(subtotal);
                    log.info("💰 Discount by amount: {}", discountAmount);
                } else if (voucher.getVoucherDiscountPercent() != null &&
                        voucher.getVoucherDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                    discountAmount = subtotal.multiply(voucher.getVoucherDiscountPercent())
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                    log.info("💰 Discount by percent: {}% = {}", voucher.getVoucherDiscountPercent(), discountAmount);
                }

                voucher.setVoucherUsedCount(voucher.getVoucherUsedCount() + 1);
                voucherRepository.save(voucher);
                appliedVoucher = voucher;

                log.info("✅ Applied voucher: {}, discount: {}", voucher.getVoucherCode(), discountAmount);
            } else {
                log.warn("⚠️ Voucher with ID {} is not valid", request.getVoucherId());
                throw new RuntimeException("Voucher không hợp lệ hoặc đã hết hạn");
            }
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setShippingAddress(null);
        order.setCartId(null);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setOrderSubtotalAmount(subtotal);
        order.setOrderDiscountAmount(discountAmount);
        order.setOrderShippingFee(DEFAULT_SHIPPING_FEE);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setNotes(request.getNotes() != null ? request.getNotes() : "");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setDeletedAt(null);
        order.setVoucher(appliedVoucher);

        BigDecimal totalAmount = subtotal.subtract(discountAmount).add(DEFAULT_SHIPPING_FEE);
        order.setOrderTotalAmount(totalAmount);

        log.info("Order summary: subtotal={}, discount={}, shipping={}, total={}",
                subtotal, discountAmount, DEFAULT_SHIPPING_FEE, totalAmount);

        Order savedOrder = orderRepository.save(order);

        for (OrderDetail detail : orderDetails) {
            detail.setOrder(savedOrder);
            orderDetailRepository.save(detail);

            ProductDetail pd = detail.getProductDetail();
            pd.setQuantity(pd.getQuantity() - detail.getQuantity());
            productDetailRepository.save(pd);
        }

        OrderHistory history = new OrderHistory();
        history.setOrder(savedOrder);
        history.setOrderStatus(savedOrder.getOrderStatus().toString());
        String note = "Đơn hàng được tạo thành công";
        if (appliedVoucher != null) {
            note += " (Áp dụng voucher: " + appliedVoucher.getVoucherCode() + ", giảm " + discountAmount + "₫)";
        }
        history.setNote(note);
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        try {
            // Không clear giỏ hàng nếu thanh toán bằng VNPAY, sẽ clear sau khi thanh toán thành công
            if (!"VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
                Cart cart = cartRepository.findByCustomerIdAndDeletedAtIsNull(customerId).orElse(null);
                if (cart != null) {
                    List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndDeletedAtIsNull(cart.getCartId());
                    if (cartItems != null && !cartItems.isEmpty()) {
                        cartItemRepository.deleteAll(cartItems);
                        cart.getCartItems().clear();
                        log.info("Cleared {} items from cart for customer: {}", cartItems.size(), customerId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not clear cart: {}", e.getMessage());
        }

        log.info("Order created successfully: {}", savedOrder.getOrderId());

        return convertToResponse(savedOrder);
    }

    // ============================================
    // 2. LẤY CHI TIẾT ĐƠN HÀNG
    // ============================================
    @Override
    public OrderResponseDTO getOrder(Long orderId, Integer customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new RuntimeException("Đơn hàng không thuộc về khách hàng này");
        }

        return convertToResponse(order);
    }

    // ============================================
    // 3. LẤY DANH SÁCH ĐƠN HÀNG CỦA KHÁCH HÀNG
    // ============================================
    @Override
    public Page<OrderResponseDTO> getMyOrders(Integer customerId, String status, Pageable pageable) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        Page<Order> orders;
        if (status != null && !status.isEmpty()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status);
                orders = orderRepository.findByCustomerAndOrderStatusAndDeletedAtIsNull(customer, orderStatus, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid order status: {}", status);
                orders = orderRepository.findByCustomerAndDeletedAtIsNull(customer, pageable);
            }
        } else {
            orders = orderRepository.findByCustomerAndDeletedAtIsNull(customer, pageable);
        }

        return orders.map(this::convertToResponse);
    }

    // ============================================
    // 4. HỦY ĐƠN HÀNG
    // ============================================
    @Override
    public OrderResponseDTO cancelOrder(Long orderId, Integer customerId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new RuntimeException("Đơn hàng không thuộc về khách hàng này");
        }

        String currentStatus = order.getOrderStatus().toString();
        if (!"PENDING_PAYMENT".equals(currentStatus) && !"PROCESSING".equals(currentStatus)) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng đang ở trạng thái 'Chờ thanh toán' hoặc 'Đang xử lý'");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(orderId);
        for (OrderDetail detail : details) {
            ProductDetail pd = detail.getProductDetail();
            pd.setQuantity(pd.getQuantity() + detail.getQuantity());
            productDetailRepository.save(pd);
        }

        Voucher voucher = order.getVoucher();
        if (voucher != null) {
            voucher.setVoucherUsedCount(Math.max(0, voucher.getVoucherUsedCount() - 1));
            voucherRepository.save(voucher);
            log.info("🔄 Restored voucher usage: {}", voucher.getVoucherCode());
        }

        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus("CANCELLED");
        history.setNote("Đơn hàng bị hủy. Lý do: " + reason);
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Order {} cancelled by customer {}, reason: {}", orderId, customerId, reason);

        return convertToResponse(order);
    }

    // ============================================
    // 5. YÊU CẦU HOÀN HÀNG (CÓ ẢNH)
    // ============================================
    @Override
    @Transactional
    public ReturnRequestResponseDTO requestReturn(Long orderId, Integer customerId, ReturnRequestDTO request) {
        log.info("🔄 Customer {} requesting return for order {}", customerId, orderId);

        // 1. Kiểm tra đơn hàng tồn tại
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // 2. Kiểm tra quyền sở hữu
        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new RuntimeException("Đơn hàng không thuộc về khách hàng này");
        }

        // 3. Kiểm tra trạng thái (chỉ cho phép khi COMPLETED)
        String currentStatus = order.getOrderStatus().toString();
        if (!"COMPLETED".equals(currentStatus)) {
            throw new RuntimeException("Chỉ có thể yêu cầu hoàn hàng khi đơn hàng đã hoàn thành");
        }

        // 4. Kiểm tra đã có yêu cầu hoàn hàng chưa
        var existingReturn = returnRequestRepository.findByOrderIdAndDeletedAtIsNull(orderId);
        if (existingReturn.isPresent()) {
            String status = existingReturn.get().getStatus();
            if ("PENDING".equals(status) || "APPROVED".equals(status) || "RETURN_PENDING".equals(status)) {
                throw new RuntimeException("Đã có yêu cầu hoàn hàng đang được xử lý");
            }
        }

        // 5. Chuyển đổi images sang JSON
        String imagesJson = null;
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                imagesJson = objectMapper.writeValueAsString(request.getImages());
                log.info("✅ Images converted to JSON: {}", imagesJson);
            } catch (Exception e) {
                log.warn("Cannot convert images to JSON: {}", e.getMessage());
            }
        }

        // 6. Tạo yêu cầu hoàn hàng
        ReturnRequest returnRequest = ReturnRequest.builder()
                .orderId(orderId)
                .customerId(customerId)
                .reason(request.getReason())
                .description(request.getDescription())
                .images(imagesJson)
                .status("PENDING")
                .build();

        returnRequest = returnRequestRepository.save(returnRequest);
        log.info("✅ Return request saved with ID: {}", returnRequest.getReturnRequestId());

        // 7. Cập nhật trạng thái đơn hàng
        order.setOrderStatus(OrderStatus.RETURN_REQUESTED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 8. Ghi lịch sử
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus("RETURN_REQUESTED");
        history.setNote("Khách hàng yêu cầu hoàn hàng. Lý do: " + request.getReason());
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Return request created for order {}", orderId);

        return convertToReturnResponse(returnRequest, order);
    }

    // ============================================
    // 6. LẤY DANH SÁCH YÊU CẦU HOÀN HÀNG
    // ============================================
    @Override
    public Page<ReturnRequestResponseDTO> getMyReturnRequests(Integer customerId, Pageable pageable) {
        return returnRequestRepository.findByCustomerIdAndDeletedAtIsNull(customerId, pageable)
                .map(r -> {
                    Order order = orderRepository.findById(r.getOrderId()).orElse(null);
                    return convertToReturnResponse(r, order);
                });
    }

    // ============================================
    // 7. KIỂM TRA VOUCHER HỢP LỆ
    // ============================================
    private boolean isValidVoucher(Voucher voucher) {
        if (voucher == null) return false;

        LocalDateTime now = LocalDateTime.now();
        boolean isValid = "ACTIVE".equals(voucher.getVoucherStatus()) &&
                voucher.getVoucherDeletedAt() == null &&
                now.isAfter(voucher.getVoucherStartDate()) &&
                now.isBefore(voucher.getVoucherEndDate()) &&
                (voucher.getVoucherMaxUsage() == null ||
                        voucher.getVoucherUsedCount() < voucher.getVoucherMaxUsage());

        if (!isValid) {
            log.warn("❌ Voucher {} is not valid: status={}, deleted={}, start={}, end={}, used={}/{}",
                    voucher.getVoucherCode(),
                    voucher.getVoucherStatus(),
                    voucher.getVoucherDeletedAt(),
                    voucher.getVoucherStartDate(),
                    voucher.getVoucherEndDate(),
                    voucher.getVoucherUsedCount(),
                    voucher.getVoucherMaxUsage());
        }

        return isValid;
    }

    // ============================================
    // 8. CHUYỂN ĐỔI ORDER -> ORDER RESPONSE DTO
    // ============================================
    private OrderResponseDTO convertToResponse(Order order) {
        List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(order.getOrderId());

        List<OrderItemResponseDTO> itemResponses = details.stream()
                .map(detail -> {
                    ProductDetail pd = detail.getProductDetail();
                    Product product = pd.getProduct();

                    String imageUrl = null;
                    if (pd.getProductImages() != null && !pd.getProductImages().isEmpty()) {
                        imageUrl = pd.getProductImages().get(0).getProductImagePath();
                    }

                    return OrderItemResponseDTO.builder()
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
        ShippingAddressDTO shippingDTO = null;
        if (addr != null) {
            shippingDTO = ShippingAddressDTO.builder()
                    .addressId(addr.getShippingAddressId().intValue())
                    .fullName(addr.getShippingAddressFullName())
                    .phone(addr.getShippingAddressPhone())
                    .addressLine(addr.getShippingAddressAddressLine())
                    .city(addr.getShippingAddressCity())
                    .district(addr.getShippingAddressDistrict())
                    .ward(addr.getShippingAddressWard())
                    .build();
        }

        Voucher voucher = order.getVoucher();
        VoucherInfoDTO voucherDTO = null;
        if (voucher != null) {
            voucherDTO = VoucherInfoDTO.builder()
                    .voucherId(voucher.getVoucherId())
                    .voucherCode(voucher.getVoucherCode())
                    .discountAmount(voucher.getVoucherDiscountAmount())
                    .discountPercent(voucher.getVoucherDiscountPercent())
                    .build();
        }

        BigDecimal discountAmount = order.getOrderDiscountAmount() != null ?
                order.getOrderDiscountAmount() : BigDecimal.ZERO;

        BigDecimal totalAmount = order.getOrderSubtotalAmount()
                .subtract(discountAmount)
                .add(order.getOrderShippingFee() != null ? order.getOrderShippingFee() : BigDecimal.ZERO);

        return OrderResponseDTO.builder()
                .orderId(order.getOrderId())
                .orderCode("ORD" + String.format("%08d", order.getOrderId()))
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus().toString())
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().toString() : PaymentStatus.UNPAID.toString())
                .subtotalAmount(order.getOrderSubtotalAmount())
                .discountAmount(discountAmount)
                .shippingFee(order.getOrderShippingFee())
                .totalAmount(totalAmount)
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .customer(CustomerInfoDTO.builder()
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

    // ============================================
    // 9. CHUYỂN ĐỔI RETURN REQUEST -> RESPONSE DTO
    // ============================================
    private ReturnRequestResponseDTO convertToReturnResponse(ReturnRequest returnRequest, Order order) {
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

        return ReturnRequestResponseDTO.builder()
                .orderId(returnRequest.getOrderId())
                .orderCode(orderCode)
                .reason(returnRequest.getReason())
                .description(returnRequest.getDescription())
                .images(images)
                .status(returnRequest.getStatus())
                .createdAt(returnRequest.getCreatedAt())
                .updatedAt(returnRequest.getUpdatedAt())
                .adminNote(returnRequest.getAdminNote())
                .build();
    }
}