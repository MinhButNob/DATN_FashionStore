package com.example.backend_fashionstore.customer.order.service;

import com.example.backend_fashionstore.QuanLyDiaChi.repository.ProvinceRepository;
import com.example.backend_fashionstore.QuanLyDiaChi.repository.WardRepository;
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
import com.example.backend_fashionstore.shipping.dto.ShippingCalculateRequest;
import com.example.backend_fashionstore.shipping.service.ShippingServiceImpl;
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
import java.time.Duration;
import java.util.*;
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
    private final ShippingServiceImpl shippingService;

    private final ProvinceRepository provinceRepository;
    private final WardRepository wardRepository;

    private static final long CANCEL_HOURS_LIMIT = 48;
    private static final long RETURN_HOURS_LIMIT = 48;

    // ============================================
    // 1. TẠO ĐƠN HÀNG
    // ============================================
    @Override
    @Transactional
    public OrderResponseDTO createOrder(Integer customerId, OrderRequestDTO request) {
        log.info("Creating order for customer: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        String shippingAddressStr = "";
        String shippingCity = "";
        String shippingDistrict = "";
        String shippingWard = "";

        // 1. ƯU TIÊN SỬ DỤNG shippingAddressId
        if (request.getShippingAddressId() != null) {
            Long addressId = request.getShippingAddressId().longValue();
            ShippingAddress shippingAddress = shippingAddressRepository.findById(addressId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ giao hàng với ID: " + request.getShippingAddressId()));

            if (!shippingAddress.getCustomer().getCustomerId().equals(customerId)) {
                throw new RuntimeException("Địa chỉ giao hàng không thuộc về khách hàng này");
            }

            shippingAddressStr = shippingAddress.getFullAddress();
            shippingCity = shippingAddress.getShippingAddressCity();
            shippingDistrict = shippingAddress.getShippingAddressDistrict();
            shippingWard = shippingAddress.getShippingAddressWard();
            if (shippingAddressStr == null || shippingAddressStr.isEmpty()) {
                shippingAddressStr = buildFullAddressLine(
                        shippingAddress.getStreetAddress(),
                        shippingWard,
                        shippingDistrict,
                        shippingCity
                );
            }
            log.info("📦 Using saved shipping address ID: {}", request.getShippingAddressId());
            log.info("   Full address: {}", shippingAddressStr);
        }
        // 2. TẠO ĐỊA CHỈ TẠM TỪ REQUEST (Dùng data Open API truyền lên)
        else if (request.getProvinceId() != null && request.getWardId() != null) {
            log.info("📦 Creating temporary address from Open API data (provinceId and wardId present)");

            // Không query database local vì ID từ Open API sẽ không khớp với local DB.
            // Dùng trực tiếp tên Thành phố và Phường xã gửi từ frontend.
            shippingCity = request.getShippingCity() != null ? request.getShippingCity() : "";
            shippingDistrict = request.getShippingDistrict() != null ? request.getShippingDistrict() : "";
            shippingWard = request.getShippingWard() != null ? request.getShippingWard() : "";

            shippingAddressStr = buildFullAddressLine(
                    request.getShippingAddress(),
                    shippingWard,
                    shippingDistrict,
                    shippingCity
            );

            log.info("📦 Temporary address created: {}", shippingAddressStr);
        }
        // 3. TẠO ĐỊA CHỈ TẠM TỪ REQUEST (cách cũ)
        else if (request.getShippingFullName() != null && !request.getShippingFullName().isEmpty()) {
            log.info("📦 Creating temporary address from shipping fields");

            shippingCity = request.getShippingCity() != null ? request.getShippingCity() : "";
            shippingDistrict = request.getShippingDistrict() != null ? request.getShippingDistrict() : "";
            shippingWard = request.getShippingWard() != null ? request.getShippingWard() : "";

            shippingAddressStr = buildFullAddressLine(
                    request.getShippingAddress(),
                    shippingWard,
                    shippingDistrict,
                    shippingCity
            );

            log.info("📦 Temporary address created: {}", shippingAddressStr);
        }
        // 4. FALLBACK: Lấy địa chỉ mặc định
        else {
            ShippingAddress shippingAddress = shippingAddressRepository
                    .findByCustomer_CustomerIdAndIsDefaultTrueAndStatus(customerId, "ACTIVE")
                    .orElseGet(() -> {
                        List<ShippingAddress> addresses = shippingAddressRepository
                                .findByCustomer_CustomerIdAndStatus(customerId, "ACTIVE");
                        if (!addresses.isEmpty()) {
                            log.info("📦 Using first available address as default");
                            return addresses.get(0);
                        }
                        return null;
                    });

            if (shippingAddress != null) {
                shippingAddressStr = shippingAddress.getFullAddress();
                shippingCity = shippingAddress.getShippingAddressCity();
                shippingDistrict = shippingAddress.getShippingAddressDistrict();
                shippingWard = shippingAddress.getShippingAddressWard();
                if (shippingAddressStr == null || shippingAddressStr.isEmpty()) {
                    shippingAddressStr = buildFullAddressLine(
                            shippingAddress.getStreetAddress(),
                            shippingWard,
                            shippingDistrict,
                            shippingCity
                    );
                }
                log.info("📦 Using default shipping address ID: {}", shippingAddress.getShippingAddressId());
                log.info("   Full address: {}", shippingAddressStr);
            } else {
                // Fallback to customer address
                if (customer.getAddress() != null && !customer.getAddress().isEmpty()) {
                    shippingAddressStr = customer.getAddress();
                    log.info("📦 Using customer's primary address: {}", shippingAddressStr);
                } else {
                    log.warn("⚠️ No shipping address found for customer: {}", customerId);
                    throw new RuntimeException("Vui lòng thêm địa chỉ giao hàng");
                }
            }
        }

        // ===== KIỂM TRA SẢN PHẨM =====
        List<OrderItemRequestDTO> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Không có sản phẩm trong đơn hàng");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderDetail> orderDetails = new ArrayList<>();
        Map<Integer, BigDecimal> cartPricesByVariant = cartRepository
                .findByCustomerIdAndDeletedAtIsNull(customerId)
                .map(Cart::getCartItems)
                .orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(
                        item -> item.getProductDetail().getProductDetailId(),
                        item -> item.getDiscountedPrice() != null
                                ? item.getDiscountedPrice() : item.getProductDetail().getPrice(),
                        (first, ignored) -> first));

        for (OrderItemRequestDTO itemReq : items) {
            ProductDetail productDetail = productDetailRepository.findById(itemReq.getProductDetailId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm: " + itemReq.getProductDetailId()));

            if (productDetail.getQuantity() < itemReq.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + productDetail.getProduct().getProductName() +
                        " không đủ số lượng. Còn: " + productDetail.getQuantity());
            }

            // Use the price snapshot shown in this customer's cart. This keeps
            // variant-specific promotions consistent with voucher allocation.
            BigDecimal unitPrice = cartPricesByVariant.getOrDefault(
                    itemReq.getProductDetailId(), productDetail.getPrice());
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

        // ===== XỬ LÝ VOUCHER =====
        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;

        if (request.getVoucherId() != null) {
            log.info("🔍 Applying voucher with ID: {}", request.getVoucherId());

            Voucher voucher = voucherRepository.findById(request.getVoucherId())
                    .orElse(null);

            if (voucher != null && isValidVoucher(voucher)) {
                if (voucher.getVoucherMinOrderValue() != null &&
                        subtotal.compareTo(voucher.getVoucherMinOrderValue()) < 0) {
                    throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher: " +
                            voucher.getVoucherMinOrderValue());
                }

                if (voucher.getVoucherDiscountAmount() != null &&
                        voucher.getVoucherDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    discountAmount = voucher.getVoucherDiscountAmount().min(subtotal);
                } else if (voucher.getVoucherDiscountPercent() != null &&
                        voucher.getVoucherDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                    discountAmount = subtotal.multiply(voucher.getVoucherDiscountPercent())
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                }

                voucher.setVoucherUsedCount(voucher.getVoucherUsedCount() + 1);
                voucherRepository.save(voucher);
                appliedVoucher = voucher;
                log.info("✅ Applied voucher: {}, discount: {}", voucher.getVoucherCode(), discountAmount);
            } else {
                throw new RuntimeException("Voucher không hợp lệ hoặc đã hết hạn");
            }
        }

        // Persist the voucher allocation on each line. The last line receives the
        // rounding remainder so that line discounts always equal the order discount.
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < orderDetails.size(); i++) {
            OrderDetail detail = orderDetails.get(i);
            BigDecimal lineSubtotal = detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
            BigDecimal lineDiscount;
            if (i == orderDetails.size() - 1) {
                lineDiscount = discountAmount.subtract(allocated);
            } else if (subtotal.signum() == 0) {
                lineDiscount = BigDecimal.ZERO;
            } else {
                lineDiscount = discountAmount.multiply(lineSubtotal)
                        .divide(subtotal, 0, RoundingMode.HALF_UP);
            }
            lineDiscount = lineDiscount.max(BigDecimal.ZERO).min(lineSubtotal);
            detail.setDiscount(lineDiscount);
            detail.setFinalAmount(lineSubtotal.subtract(lineDiscount));
            allocated = allocated.add(lineDiscount);
        }

        BigDecimal shippingFee = calculateShippingFee(customerId, request, shippingAddressStr,
                shippingCity, shippingDistrict, shippingWard, subtotal.subtract(discountAmount));

        // ===== TẠO ĐƠN HÀNG =====
        Order order = new Order();
        order.setCustomer(customer);
        order.setShippingAddress(shippingAddressStr);
        order.setShippingCity(shippingCity);
        order.setShippingDistrict(shippingDistrict);
        order.setShippingWard(shippingWard);
        order.setCartId(null);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setOrderSubtotalAmount(subtotal);
        order.setOrderDiscountAmount(discountAmount);
        order.setOrderShippingFee(shippingFee);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setNotes(request.getNotes() != null ? request.getNotes() : "");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setDeletedAt(null);
        order.setVoucher(appliedVoucher);

        BigDecimal totalAmount = subtotal.subtract(discountAmount).add(shippingFee);
        order.setOrderTotalAmount(totalAmount);

        log.info("Order summary: subtotal={}, discount={}, shipping={}, total={}",
                subtotal, discountAmount, shippingFee, totalAmount);

        Order savedOrder = orderRepository.save(order);

        // ===== LƯU ORDER DETAILS =====
        for (OrderDetail detail : orderDetails) {
            detail.setOrder(savedOrder);
            orderDetailRepository.save(detail);
        }

        // ===== GHI LỊCH SỬ =====
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

        // ===== XÓA GIỎ HÀNG =====
        try {
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

    private BigDecimal calculateShippingFee(Integer customerId, OrderRequestDTO request,
                                            String address, String province, String district,
                                            String ward, BigDecimal subtotal) {
        ShippingCalculateRequest quoteRequest = ShippingCalculateRequest.builder()
                .province(province).district(district).ward(ward).address(address)
                .orderValue(subtotal)
                .shippingFee(request.getShippingFee()).build();
        try {
            return shippingService.calculate(customerId, quoteRequest).getShippingFee();
        } catch (Exception ex) {
            if (request.getShippingFee() != null && request.getShippingFee().compareTo(BigDecimal.ZERO) >= 0) {
                log.warn("GHTK unavailable while creating order; using checkout quote {}. reason={}",
                        request.getShippingFee(), ex.getMessage());
                return request.getShippingFee();
            }
            log.error("Cannot calculate shipping fee from GHTK", ex);
            throw new RuntimeException("Không thể tính phí vận chuyển. Vui lòng thử lại.", ex);
        }
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
    // 3. LẤY DANH SÁCH ĐƠN HÀNG
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
        log.info("🔄 Customer {} requesting to cancel order {}", customerId, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new RuntimeException("Đơn hàng không thuộc về khách hàng này");
        }

        String currentStatus = order.getOrderStatus().toString();

        if (!"PENDING_PAYMENT".equals(currentStatus) && !"PROCESSING".equals(currentStatus)) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng đang ở trạng thái 'Chờ thanh toán' hoặc 'Đang xử lý'");
        }

        LocalDateTime orderCreatedAt = order.getCreatedAt();
        LocalDateTime now = LocalDateTime.now();
        long hoursPassed = Duration.between(orderCreatedAt, now).toHours();

        if (hoursPassed > CANCEL_HOURS_LIMIT) {
            throw new RuntimeException("Không thể hủy đơn hàng sau " + CANCEL_HOURS_LIMIT + " giờ kể từ lúc đặt. Vui lòng liên hệ hỗ trợ.");
        }

        log.info("✅ Order {} can be cancelled. Hours passed: {}/{}", orderId, hoursPassed, CANCEL_HOURS_LIMIT);

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        // ✅ Ghi nhận loại hủy để thống kê "Thống kê hủy đơn" đếm đúng (countCancelledByType LIKE %tag%)
        String existingNotes = order.getNotes();
        order.setNotes((existingNotes != null && !existingNotes.isBlank() ? existingNotes + " | " : "")
                + "Khách hàng hủy");
        orderRepository.save(order);

        if (isStockDeducted(currentStatus)) {
            List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(orderId);
            for (OrderDetail detail : details) {
                ProductDetail pd = detail.getProductDetail();
                pd.setQuantity(pd.getQuantity() + detail.getQuantity());
                productDetailRepository.save(pd);
                log.info("✅ Restored {} units for product detail: {} (order cancelled)",
                        detail.getQuantity(), pd.getProductDetailId());
            }
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
        history.setNote("Khách hàng hủy đơn. Lý do: " + reason + ". (Thời gian đặt: " + orderCreatedAt + ")");
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Order {} cancelled by customer {}, reason: {}", orderId, customerId, reason);

        return convertToResponse(order);
    }

    // ============================================
    // 5. YÊU CẦU HOÀN HÀNG
    // ============================================
    @Override
    @Transactional
    public ReturnRequestResponseDTO requestReturn(Long orderId, Integer customerId, ReturnRequestDTO request) {
        log.info("🔄 Customer {} requesting return for order {}", customerId, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new RuntimeException("Đơn hàng không thuộc về khách hàng này");
        }

        String currentStatus = order.getOrderStatus().toString();

        // ===== KIỂM TRA THỜI GIAN =====
        LocalDateTime orderDate = order.getOrderDate() != null ? order.getOrderDate() : order.getCreatedAt();
        LocalDateTime now = LocalDateTime.now();
        long hoursPassed = Duration.between(orderDate, now).toHours();
        boolean isWithin48Hours = hoursPassed <= RETURN_HOURS_LIMIT;

        log.info("⏰ Order {} - Hours passed: {}/{}", orderId, hoursPassed, RETURN_HOURS_LIMIT);
        log.info("📊 Order status: {}, isWithin48Hours: {}", currentStatus, isWithin48Hours);

        // ===== KIỂM TRA ĐIỀU KIỆN HOÀN HÀNG =====
        boolean canReturn = false;
        String errorMessage = null;

        // Trường hợp 1: Đơn hàng đã giao hoặc hoàn thành (có thể hoàn bất kỳ lúc nào)
        if ("DELIVERED".equals(currentStatus) || "COMPLETED".equals(currentStatus) ||
                "PARTIALLY_RETURNED".equals(currentStatus)) {
            canReturn = true;
            log.info("✅ Order {} is DELIVERED/COMPLETED - can return", orderId);
        }
        // Trường hợp 2: Đơn hàng đang giao và trong vòng 48h
        else if ("SHIPPING".equals(currentStatus) && isWithin48Hours) {
            canReturn = true;
            log.info("✅ Order {} is SHIPPING and within 48h - can return", orderId);
        }
        // Trường hợp 3: Đơn hàng đã xác nhận hoặc đang xử lý và trong vòng 48h
        else if (("CONFIRMED".equals(currentStatus) || "PROCESSING".equals(currentStatus)) && isWithin48Hours) {
            canReturn = true;
            log.info("✅ Order {} is CONFIRMED/PROCESSING and within 48h - can return", orderId);
        }
        // Trường hợp 4: Đơn hàng chờ thanh toán - khuyến khích hủy thay vì hoàn
        else if ("PENDING_PAYMENT".equals(currentStatus)) {
            errorMessage = "Đơn hàng chưa được xác nhận. Bạn có thể hủy đơn hàng thay vì yêu cầu hoàn.";
            log.warn("⚠️ Order {} is PENDING_PAYMENT - suggest cancel instead", orderId);
        }
        // Trường hợp 5: Đơn hàng đang giao nhưng quá 48h
        else if ("SHIPPING".equals(currentStatus) && !isWithin48Hours) {
            errorMessage = "Đơn hàng đang được giao và đã quá 48h kể từ lúc đặt. Vui lòng đợi đến khi nhận hàng để yêu cầu hoàn.";
            log.warn("⚠️ Order {} is SHIPPING but exceeded 48h", orderId);
        }
        // Trường hợp 6: Đơn hàng đã hủy
        else if ("CANCELLED".equals(currentStatus)) {
            errorMessage = "Đơn hàng đã bị hủy. Không thể yêu cầu hoàn.";
            log.warn("⚠️ Order {} is CANCELLED", orderId);
        }
        // Trường hợp 7: Đơn hàng đã yêu cầu hoàn
        else if ("RETURN_REQUESTED".equals(currentStatus) || "RETURN_APPROVED".equals(currentStatus) ||
                "RETURN_RECEIVED".equals(currentStatus) || "RETURN_PENDING".equals(currentStatus) ||
                "RETURNED".equals(currentStatus) || "REJECTED".equals(currentStatus)) {
            errorMessage = "Đơn hàng đã có yêu cầu hoàn hàng. Vui lòng kiểm tra lại.";
            log.warn("⚠️ Order {} already has return request", orderId);
        }
        // Trường hợp 8: Không đủ điều kiện
        else {
            errorMessage = "Không thể yêu cầu hoàn hàng. Đơn hàng đang ở trạng thái '" +
                    getStatusText(currentStatus) + "'. " +
                    "Yêu cầu hoàn hàng chỉ áp dụng khi đơn hàng đã được giao hoặc trong vòng 48h kể từ lúc đặt.";
            log.warn("⚠️ Order {} cannot return - status: {}, within48h: {}", orderId, currentStatus, isWithin48Hours);
        }

        if (!canReturn) {
            throw new RuntimeException(errorMessage);
        }

        // ===== KIỂM TRA ĐÃ CÓ YÊU CẦU HOÀN HÀNG CHƯA =====
        var existingReturn = returnRequestRepository.findByOrderIdAndDeletedAtIsNull(orderId);
        if (existingReturn.isPresent()) {
            String status = existingReturn.get().getStatus();
            if ("PENDING".equals(status) || "APPROVED".equals(status) || "RECEIVED".equals(status)) {
                throw new RuntimeException("Đã có yêu cầu hoàn hàng đang được xử lý");
            }
        }

        // ===== LẤY DANH SÁCH SẢN PHẨM TRONG ĐƠN HÀNG =====
        List<OrderDetail> orderDetails = orderDetailRepository.findByOrder_OrderId(orderId);

        // ===== KIỂM TRA CÁC productDetailIds CÓ TỒN TẠI TRONG ĐƠN HÀNG KHÔNG =====
        List<Integer> requestedIds = request.getProductDetailIds();
        Map<Integer, Integer> requestedQuantities = request.getQuantities();

        if (requestedIds == null || requestedIds.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất 1 sản phẩm để hoàn");
        }

        // ===== KIỂM TRA TỪNG SẢN PHẨM =====
        for (Integer productDetailId : requestedIds) {
            OrderDetail detail = orderDetails.stream()
                    .filter(d -> d.getProductDetail().getProductDetailId().equals(productDetailId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong đơn hàng: " + productDetailId));

            // Kiểm tra số lượng hoàn không vượt quá số lượng đã mua
            Integer returnQty = requestedQuantities != null ? requestedQuantities.getOrDefault(productDetailId, 0) : 0;
            if (returnQty <= 0) {
                throw new RuntimeException("Số lượng hoàn phải lớn hơn 0 cho sản phẩm: " + productDetailId);
            }
            if (detail.getStatus() != null && detail.getStatus() != com.example.backend_fashionstore.enums.OrderItemStatus.COMPLETED) {
                throw new RuntimeException("Invalid return item status: " + productDetailId);
            }
            if (returnQty > detail.getQuantity()) {
                throw new RuntimeException("Số lượng hoàn vượt quá số lượng đã mua cho sản phẩm: " + productDetailId);
            }
        }

        // ===== CHUYỂN DANH SÁCH productDetailIds THÀNH JSON =====
        String productDetailIdsJson = null;
        String quantitiesJson = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            productDetailIdsJson = objectMapper.writeValueAsString(request.getProductDetailIds());
            quantitiesJson = objectMapper.writeValueAsString(request.getQuantities());
            log.info("✅ ProductDetailIds JSON: {}", productDetailIdsJson);
            log.info("✅ Quantities JSON: {}", quantitiesJson);
        } catch (Exception e) {
            log.warn("Cannot convert to JSON: {}", e.getMessage());
        }

        String imagesJson = null;
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                imagesJson = objectMapper.writeValueAsString(request.getImages());
                log.info("✅ Images JSON: {}", imagesJson);
            } catch (Exception e) {
                log.warn("Cannot convert images to JSON: {}", e.getMessage());
            }
        }

        ReturnRequest returnRequest = ReturnRequest.builder()
                .orderId(orderId)
                .customerId(customerId)
                .productDetailIds(productDetailIdsJson)
                .quantities(quantitiesJson)
                .reason(request.getReason())
                .description(request.getDescription())
                .images(imagesJson)
                .status("PENDING")
                .build();

        returnRequest = returnRequestRepository.save(returnRequest);
        log.info("✅ Return request saved with ID: {}", returnRequest.getReturnRequestId());

        // Mark only the requested lines; other lines remain completed.
        for (OrderDetail detail : orderDetails) {
            Integer productDetailId = detail.getProductDetail().getProductDetailId();
            if (requestedIds.contains(detail.getProductDetail().getProductDetailId())) {
                detail.setStatus(com.example.backend_fashionstore.enums.OrderItemStatus.RETURN_REQUESTED);
                orderDetailRepository.save(detail);
            }
            if (false && detail.getStatus() != null && detail.getStatus() != com.example.backend_fashionstore.enums.OrderItemStatus.COMPLETED) {
                throw new RuntimeException("Sản phẩm đã có yêu cầu hoàn hoặc đã hoàn: " + productDetailId);
            }
        }

        // ===== CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG =====
        order.setOrderStatus(OrderStatus.RETURN_REQUESTED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // ===== GHI LỊCH SỬ =====
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus("RETURN_REQUESTED");
        history.setNote("Khách hàng yêu cầu hoàn " + request.getProductDetailIds().size() + " sản phẩm. Lý do: " + request.getReason() +
                ". (Thời gian đặt: " + orderDate + ", đã qua " + hoursPassed + "h)");
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
    // 7. LẤY CHI TIẾT YÊU CẦU HOÀN HÀNG
    // ============================================
    @Override
    public ReturnRequestResponseDTO getReturnRequestDetail(Long orderId, Integer customerId) {
        ReturnRequest returnRequest = returnRequestRepository
                .findFirstByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn hàng"));

        if (!returnRequest.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Yêu cầu hoàn hàng không thuộc về khách hàng này");
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        return convertToReturnResponse(returnRequest, order);
    }

    // ============================================
    // 8. TRỪ KHO
    // ============================================
    @Override
    @Transactional
    public void deductStockForOrder(Long orderId) {
        log.info("📦 Deducting stock for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!"PROCESSING".equals(order.getOrderStatus().toString())) {
            log.warn("Order {} is not in PROCESSING status, current: {}", orderId, order.getOrderStatus());
            return;
        }

        List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(orderId);
        for (OrderDetail detail : details) {
            ProductDetail pd = detail.getProductDetail();
            if (pd.getQuantity() < detail.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + pd.getProduct().getProductName() +
                        " không đủ số lượng để giao. Còn: " + pd.getQuantity());
            }
            pd.setQuantity(pd.getQuantity() - detail.getQuantity());
            productDetailRepository.save(pd);
            log.info("✅ Deducted {} units from product detail: {}", detail.getQuantity(), pd.getProductDetailId());
        }

        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setOrderStatus(order.getOrderStatus().toString());
        history.setNote("Đã trừ số lượng sản phẩm trong kho");
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        log.info("✅ Stock deducted successfully for order: {}", orderId);
    }

    // ============================================
    // 9. HOÀN KHO
    // ============================================
    @Override
    @Transactional
    public void restoreStockForOrder(Long orderId) {
        log.info("📦 Restoring stock for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(orderId);
        for (OrderDetail detail : details) {
            ProductDetail pd = detail.getProductDetail();
            pd.setQuantity(pd.getQuantity() + detail.getQuantity());
            productDetailRepository.save(pd);
            log.info("✅ Restored {} units to product detail: {}", detail.getQuantity(), pd.getProductDetailId());
        }

        log.info("✅ Stock restored successfully for order: {}", orderId);
    }

    // ============================================
    // 10. KIỂM TRA VOUCHER
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
    // 11. KIỂM TRA ĐÃ TRỪ KHO
    // ============================================
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

    // ============================================
    // 12. LẤY TEXT TRẠNG THÁI
    // ============================================
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
        statusMap.put("RETURNED", "Đã hoàn trả");
        statusMap.put("REJECTED", "Từ chối hoàn trả");
        return statusMap.getOrDefault(status, status);
    }

    // ============================================
    // 13. CHUYỂN ĐỔI ORDER -> ORDER RESPONSE (ĐÃ SỬA)
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
                            .status(detail.getStatus() != null ? detail.getStatus().name() : "COMPLETED")
                            .build();
                })
                .collect(Collectors.toList());

        // ✅ LẤY SHIPPING ADDRESS TỪ ORDER (Nay là String)
        String shippingAddressStr = order.getShippingAddress();
        ShippingAddressDTO shippingDTO = null;

        if (shippingAddressStr != null && !shippingAddressStr.isEmpty()) {
            String city = order.getShippingCity() != null ? order.getShippingCity() : "";
            String district = order.getShippingDistrict() != null ? order.getShippingDistrict() : "";
            String ward = order.getShippingWard() != null ? order.getShippingWard() : "";

            shippingDTO = ShippingAddressDTO.builder()
                    .addressId(null)
                    .fullName(order.getCustomer() != null ? order.getCustomer().getCustomerName() : "")
                    .phone(order.getCustomer() != null ? order.getCustomer().getPhone() : "")
                    .addressLine(shippingAddressStr)
                    .city(city)
                    .district(district)
                    .ward(ward)
                    .fullAddress(shippingAddressStr) // ✅ ĐỊA CHỈ ĐẦY ĐỦ
                    .build();
        } else {
            // Fallback: lấy từ customer
            Customer customer = order.getCustomer();
            if (customer != null) {
                shippingDTO = ShippingAddressDTO.builder()
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

        List<ReturnRequestResponseDTO.ReturnItemDTO> returnItems = null;
        if (isReturnOrderStatus(order.getOrderStatus())) {
            returnItems = returnRequestRepository.findFirstByOrderIdAndDeletedAtIsNull(order.getOrderId())
                    .map(request -> convertToReturnResponse(request, order).getReturnItems())
                    .orElse(Collections.emptyList());
        }

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
                .refundAmount(order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO)
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
                .returnItems(returnItems)
                .build();
    }

    private boolean isReturnOrderStatus(OrderStatus status) {
        return status == OrderStatus.RETURN_REQUESTED
                || status == OrderStatus.RETURN_APPROVED
                || status == OrderStatus.RETURN_RECEIVED
                || status == OrderStatus.RETURN_PENDING
                || status == OrderStatus.RETURNED
                || status == OrderStatus.REJECTED;
    }

    // ============================================
    // 14. XÂY DỰNG ĐỊA CHỈ ĐẦY ĐỦ
    // ============================================
    private String buildFullAddressLine(String streetAddress, String ward, String district, String city) {
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

    // ============================================
    // 15. CHUYỂN ĐỔI RETURN REQUEST -> RESPONSE
    // ============================================
    private ReturnRequestResponseDTO convertToReturnResponse(ReturnRequest returnRequest, Order order) {
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

        // Parse productDetailIds và quantities
        List<Integer> productDetailIds = null;
        Map<Integer, Integer> quantities = null;
        List<ReturnRequestResponseDTO.ReturnItemDTO> returnItems = null;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            if (returnRequest.getProductDetailIds() != null && !returnRequest.getProductDetailIds().isEmpty()) {
                productDetailIds = objectMapper.readValue(
                        returnRequest.getProductDetailIds(),
                        new TypeReference<List<Integer>>() {}
                );
            }
            if (returnRequest.getQuantities() != null && !returnRequest.getQuantities().isEmpty()) {
                quantities = objectMapper.readValue(
                        returnRequest.getQuantities(),
                        new TypeReference<Map<Integer, Integer>>() {}
                );
            }

            // Lấy chi tiết sản phẩm
            if (productDetailIds != null && !productDetailIds.isEmpty() && order != null) {
                returnItems = new ArrayList<>();
                List<OrderDetail> orderDetails = orderDetailRepository.findByOrder_OrderId(order.getOrderId());

                for (Integer pdId : productDetailIds) {
                    OrderDetail detail = orderDetails.stream()
                            .filter(d -> d.getProductDetail().getProductDetailId().equals(pdId))
                            .findFirst()
                            .orElse(null);

                    if (detail != null) {
                        ProductDetail pd = detail.getProductDetail();
                        Product product = pd.getProduct();
                        Integer quantity = quantities != null ? quantities.getOrDefault(pdId, 0) : 0;

                        String imageUrl = null;
                        if (pd.getProductImages() != null && !pd.getProductImages().isEmpty()) {
                            imageUrl = pd.getProductImages().get(0).getProductImagePath();
                        }

                        // Tính số tiền hoàn lại
                        BigDecimal finalUnitPrice = detail.getFinalAmount()
                                .divide(BigDecimal.valueOf(detail.getQuantity()), 2, RoundingMode.HALF_UP);
                        BigDecimal refundAmount = finalUnitPrice.multiply(BigDecimal.valueOf(quantity));

                        returnItems.add(ReturnRequestResponseDTO.ReturnItemDTO.builder()
                                .productDetailId(pdId)
                                .productName(product.getProductName())
                                .colorName(pd.getColor() != null ? pd.getColor().getColorName() : null)
                                .sizeName(pd.getSize() != null ? pd.getSize().getSizeName() : null)
                                .imageUrl(imageUrl)
                                .quantity(quantity)
                                .unitPrice(detail.getUnitPrice())
                                .discountAmount(detail.getDiscount())
                                .finalUnitPrice(finalUnitPrice)
                                .refundAmount(refundAmount)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Cannot parse product details: {}", e.getMessage());
        }

        return ReturnRequestResponseDTO.builder()
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
                .build();
    }
}
