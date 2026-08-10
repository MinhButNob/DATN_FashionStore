package com.example.backend_fashionstore.entity;

import com.example.backend_fashionstore.enums.OrderStatus;
import com.example.backend_fashionstore.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_customer_id", nullable = false)
    private Customer customer;

    @Column(name = "shipping_address", length = 1000)
    private String shippingAddress;

    @Column(name = "shipping_city", length = 100)
    private String shippingCity;

    @Column(name = "shipping_district", length = 100)
    private String shippingDistrict;

    @Column(name = "shipping_ward", length = 100)
    private String shippingWard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_voucher_id")
    private Voucher voucher;

    @Column(name = "order_cart_id")
    private Integer cartId;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    @Column(name = "order_subtotal_amount", nullable = false)
    private BigDecimal orderSubtotalAmount;

    @Column(name = "order_discount_amount", nullable = false)
    private BigDecimal orderDiscountAmount;

    @Column(name = "order_shipping_fee", nullable = false)
    private BigDecimal orderShippingFee;

    @Column(name = "order_total_amount", nullable = true)
    private BigDecimal orderTotalAmount;

    @Column(name = "order_refund_amount", nullable = false)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "order_notes")
    private String notes;

    @Column(name = "order_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "order_updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "order_deleted_at")
    private LocalDateTime deletedAt;

    // ✅ THÊM TRƯỜNG EMAIL NGƯỜI XÁC NHẬN
    @Column(name = "order_processed_by", length = 100)
    private String processedBy;

    @Column(name = "order_processed_by_email", length = 255)  // ✅ THÊM MỚI
    private String processedByEmail;

    @Column(name = "order_processed_by_role", length = 50)
    private String processedByRole;

    @Column(name = "order_processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (refundAmount == null) {
            refundAmount = BigDecimal.ZERO;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (orderDate == null) {
            orderDate = now;
        }
        if (orderStatus == null) {
            orderStatus = OrderStatus.PENDING_PAYMENT;
        }
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.UNPAID;
        }
        if (orderDiscountAmount == null) {
            orderDiscountAmount = BigDecimal.ZERO;
        }
        if (orderShippingFee == null) {
            orderShippingFee = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}