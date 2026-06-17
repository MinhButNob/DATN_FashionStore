package com.example.backend_fashionstore.customer.order.service;

import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderDetailRepository;
import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderRepository;
import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import com.example.backend_fashionstore.auth.repository.CustomerRepository;
import com.example.backend_fashionstore.customer.cart.repository.CartItemRepository;  // ✅ THÊM
import com.example.backend_fashionstore.customer.cart.repository.CartRepository;
import com.example.backend_fashionstore.customer.order.dto.request.OrderRequestDTO;
import com.example.backend_fashionstore.customer.order.dto.request.OrderItemRequestDTO;
import com.example.backend_fashionstore.customer.order.dto.response.OrderResponseDTO;
import com.example.backend_fashionstore.customer.order.dto.response.CustomerInfoDTO;
import com.example.backend_fashionstore.customer.order.dto.response.ShippingAddressDTO;
import com.example.backend_fashionstore.customer.order.dto.response.VoucherInfoDTO;
import com.example.backend_fashionstore.customer.order.dto.response.OrderItemResponseDTO;
import com.example.backend_fashionstore.customer.order.repository.OrderHistoryRepository;
import com.example.backend_fashionstore.customer.order.repository.ShippingAddressRepository;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.quanLyVoucher.repository.VoucherRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final CartItemRepository cartItemRepository;  // ✅ THÊM

    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("30000");

    @Override
    @Transactional
    public OrderResponseDTO createOrder(Integer customerId, OrderRequestDTO request) {
        log.info("Creating order for customer: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        // ✅ LẤY ITEMS TỪ REQUEST
        List<OrderItemRequestDTO> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Không có sản phẩm trong đơn hàng");
        }

        // ✅ TÍNH TOÁN ĐƠN HÀNG
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderDetail> orderDetails = new ArrayList<>();

        for (OrderItemRequestDTO itemReq : items) {
            ProductDetail productDetail = productDetailRepository.findById(itemReq.getProductDetailId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm: " + itemReq.getProductDetailId()));

            if (productDetail.getQuantity() < itemReq.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + productDetail.getProduct().getProductName() +
                        " không đủ số lượng. Còn: " + productDetail.getQuantity());
            }

            BigDecimal itemTotal = productDetail.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(itemTotal);

            OrderDetail detail = new OrderDetail();
            detail.setProductDetail(productDetail);
            detail.setQuantity(itemReq.getQuantity());
            detail.setUnitPrice(productDetail.getPrice());
            detail.setDiscount(BigDecimal.ZERO);
            detail.setFinalAmount(itemTotal);
            orderDetails.add(detail);
        }

        // ✅ TẠO ORDER
        Order order = new Order();
        order.setCustomer(customer);
        order.setShippingAddress(null);
        order.setCartId(null);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus("PENDING_PAYMENT");
        order.setOrderSubtotalAmount(subtotal);
        order.setOrderDiscountAmount(BigDecimal.ZERO);
        order.setOrderShippingFee(DEFAULT_SHIPPING_FEE);
        order.setPaymentStatus("UNPAID");
        order.setNotes(request.getNotes() != null ? request.getNotes() : "");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setDeletedAt(null);
        order.setVoucher(null);

        BigDecimal totalAmount = subtotal.add(DEFAULT_SHIPPING_FEE);
        order.setOrderTotalAmount(totalAmount);

        log.info("Order to save: customer={}, subtotal={}, total={}", customerId, subtotal, totalAmount);

        Order savedOrder = orderRepository.save(order);

        // ✅ LƯU ORDER DETAILS
        for (OrderDetail detail : orderDetails) {
            detail.setOrder(savedOrder);
            orderDetailRepository.save(detail);

            ProductDetail pd = detail.getProductDetail();
            pd.setQuantity(pd.getQuantity() - detail.getQuantity());
            productDetailRepository.save(pd);
        }

        // ✅ LƯU ORDER HISTORY
        OrderHistory history = new OrderHistory();
        history.setOrder(savedOrder);
        history.setOrderStatus(savedOrder.getOrderStatus());
        history.setNote("Đơn hàng được tạo thành công");
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        // ✅ XÓA GIỎ HÀNG SAU KHI TẠO ĐƠN THÀNH CÔNG
        try {
            Cart cart = cartRepository.findByCustomerIdAndDeletedAtIsNull(customerId).orElse(null);
            if (cart != null) {
                // Xóa tất cả CartItem của cart
                List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndDeletedAtIsNull(cart.getCartId());
                if (cartItems != null && !cartItems.isEmpty()) {
                    cartItemRepository.deleteAll(cartItems);
                    cart.getCartItems().clear();
                    log.info("Cleared {} items from cart for customer: {}", cartItems.size(), customerId);
                }
            }
        } catch (Exception e) {
            log.warn("Could not clear cart: {}", e.getMessage());
        }

        log.info("Order created successfully: {}", savedOrder.getOrderId());

        return convertToResponse(savedOrder);
    }

    @Override
    public OrderResponseDTO getOrder(Long orderId, Integer customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new RuntimeException("Đơn hàng không thuộc về khách hàng này");
        }

        return convertToResponse(order);
    }

    @Override
    public Page<OrderResponseDTO> getMyOrders(Integer customerId, String status, Pageable pageable) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        Page<Order> orders;
        if (status != null && !status.isEmpty()) {
            orders = orderRepository.findByCustomerAndOrderStatusAndDeletedAtIsNull(customer, status, pageable);
        } else {
            orders = orderRepository.findByCustomerAndDeletedAtIsNull(customer, pageable);
        }

        return orders.map(this::convertToResponse);
    }

    @Override
    public OrderResponseDTO cancelOrder(Long orderId, Integer customerId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new RuntimeException("Đơn hàng không thuộc về khách hàng này");
        }

        if (!order.getOrderStatus().equals("PENDING_PAYMENT")) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng đang chờ thanh toán");
        }

        order.setOrderStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(orderId);
        for (OrderDetail detail : details) {
            ProductDetail pd = detail.getProductDetail();
            pd.setQuantity(pd.getQuantity() + detail.getQuantity());
            productDetailRepository.save(pd);
        }

        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setNote("Đơn hàng bị hủy. Lý do: " + reason);
        history.setTimestamp(LocalDateTime.now());
        orderHistoryRepository.save(history);

        return convertToResponse(order);
    }

    private boolean isValidVoucher(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        return "ACTIVE".equals(voucher.getVoucherStatus()) &&
                voucher.getVoucherDeletedAt() == null &&
                now.isAfter(voucher.getVoucherStartDate()) &&
                now.isBefore(voucher.getVoucherEndDate());
    }

    private BigDecimal calculateVoucherDiscount(List<OrderItemRequestDTO> items, Voucher voucher) {
        BigDecimal subtotal = items.stream()
                .map(item -> {
                    ProductDetail pd = productDetailRepository.findById(item.getProductDetailId()).orElse(null);
                    if (pd == null) return BigDecimal.ZERO;
                    return pd.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (voucher.getVoucherMinOrderValue() != null && subtotal.compareTo(voucher.getVoucherMinOrderValue()) < 0) {
            return BigDecimal.ZERO;
        }

        if (voucher.getVoucherDiscountAmount() != null) {
            return voucher.getVoucherDiscountAmount().min(subtotal);
        } else if (voucher.getVoucherDiscountPercent() != null) {
            BigDecimal discount = subtotal.multiply(voucher.getVoucherDiscountPercent())
                    .divide(BigDecimal.valueOf(100));
            return discount.min(subtotal);
        }
        return BigDecimal.ZERO;
    }

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

        BigDecimal totalAmount = order.getOrderSubtotalAmount()
                .subtract(order.getOrderDiscountAmount() != null ? order.getOrderDiscountAmount() : BigDecimal.ZERO)
                .add(order.getOrderShippingFee() != null ? order.getOrderShippingFee() : BigDecimal.ZERO);

        return OrderResponseDTO.builder()
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
}