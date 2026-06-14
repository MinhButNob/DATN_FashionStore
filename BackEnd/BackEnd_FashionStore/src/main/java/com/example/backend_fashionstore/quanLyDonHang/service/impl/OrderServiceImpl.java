package com.example.backend_fashionstore.quanLyDonHang.service.impl;

import com.example.backend_fashionstore.entity.Order;
import com.example.backend_fashionstore.entity.OrderDetail;
import com.example.backend_fashionstore.entity.Payment;
import com.example.backend_fashionstore.entity.ProductDetail;
import com.example.backend_fashionstore.quanLyDonHang.dto.OrderItemResponse;
import com.example.backend_fashionstore.quanLyDonHang.dto.OrderResponse;
import com.example.backend_fashionstore.quanLyDonHang.repository.OrderDetailRepository;
import com.example.backend_fashionstore.quanLyDonHang.repository.OrderRepository;
import com.example.backend_fashionstore.quanLyDonHang.repository.PaymentRepository;
import com.example.backend_fashionstore.quanLyDonHang.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        return orders.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        order.setOrderStatus(newStatus);
        orderRepository.save(order);
        return mapToResponse(order);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(order.getOrderId());
        List<Payment> payments = paymentRepository.findByOrder_OrderIdOrderByPaymentDateDesc(order.getOrderId());

        List<OrderItemResponse> itemResponses = details.stream().map(detail -> {
            ProductDetail pd = detail.getProductDetail();
            String imageUrl = "default_image";
            if (pd.getProductImages() != null && !pd.getProductImages().isEmpty()) {
                imageUrl = pd.getProductImages().get(0).getProductImagePath();
            }

            return OrderItemResponse.builder()
                    .id(detail.getOrderDetailId())
                    .name(pd.getProduct() != null ? pd.getProduct().getProductName() : "Unknown")
                    .image(imageUrl)
                    .price(detail.getUnitPrice())
                    .quantity(detail.getQuantity())
                    .size(pd.getSize() != null ? pd.getSize().getSizeName() : "")
                    .color(pd.getColor() != null ? pd.getColor().getColorName() : "")
                    .build();
        }).collect(Collectors.toList());

        OrderResponse.PaymentInfo paymentInfo = OrderResponse.PaymentInfo.builder()
                .method("COD")
                .status("pending")
                .build();

        if (!payments.isEmpty()) {
            Payment latestPayment = payments.get(0);
            paymentInfo.setMethod(latestPayment.getPaymentMethod() != null ? latestPayment.getPaymentMethod().getMethodName() : "COD");
            paymentInfo.setStatus(latestPayment.getPaymentStatus());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        List<OrderResponse.TimelineEvent> timeline = new ArrayList<>();
        timeline.add(OrderResponse.TimelineEvent.builder()
                .date(order.getCreatedAt() != null ? order.getCreatedAt().format(formatter) : "")
                .text("Đơn hàng được tạo")
                .icon("fas fa-plus-circle")
                .build());

        return OrderResponse.builder()
                .id("DH" + order.getOrderId())
                .originalId(order.getOrderId())
                .customer(OrderResponse.CustomerInfo.builder()
                        .name(order.getCustomer() != null ? order.getCustomer().getCustomerName() : "Khách lẻ")
                        .phone(order.getCustomer() != null ? order.getCustomer().getPhone() : "")
                        .email(order.getCustomer() != null && order.getCustomer().getUser() != null ? order.getCustomer().getUser().getEmail() : "")
                        .build())
                .items(itemResponses)
                .subtotal(order.getSubtotalAmount())
                .shipping(OrderResponse.ShippingInfo.builder()
                        .method("Giao hàng tiêu chuẩn")
                        .fee(order.getShippingFee())
                        .build())
                .discount(order.getDiscountAmount())
                .total(order.getSubtotalAmount().add(order.getShippingFee()).subtract(order.getDiscountAmount() != null ? order.getDiscountAmount() : java.math.BigDecimal.ZERO))
                .date(order.getOrderDate() != null ? order.getOrderDate().format(formatter) : "")
                .status(order.getOrderStatus())
                .payment(paymentInfo)
                .shippingAddress(order.getShippingAddress() != null ? order.getShippingAddress().getAddressLine() : "")
                .notes(order.getNotes())
                .timeline(timeline)
                .build();
    }
}
