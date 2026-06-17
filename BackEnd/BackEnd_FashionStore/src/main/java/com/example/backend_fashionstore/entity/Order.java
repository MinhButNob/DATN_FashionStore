package com.example.backend_fashionstore.entity;

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


//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "customer_id")

    @ManyToOne
    @JoinColumn(name = "order_customer_id", nullable = false)

    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "order_shipping_address_id", nullable = true)  // ✅ Cho phép null
    private ShippingAddress shippingAddress;

    @ManyToOne
    @JoinColumn(name = "order_voucher_id", nullable = true)  // ✅ Cho phép null
    private Voucher voucher;

    @Column(name = "order_cart_id", nullable = true)  // ✅ Cho phép null
    private Integer cartId;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "order_status", nullable = false)
    private String orderStatus;

    @Column(name = "order_subtotal_amount", nullable = false)
    private BigDecimal orderSubtotalAmount;

    @Column(name = "order_discount_amount", nullable = false)
    private BigDecimal orderDiscountAmount;

    @Column(name = "order_shipping_fee", nullable = false)
    private BigDecimal orderShippingFee;

    @Column(name = "order_total_amount")
    private BigDecimal orderTotalAmount;

    @Column(name = "order_payment_status", nullable = false)
    private String paymentStatus;

    @Column(name = "order_notes")
    private String notes;

    @Column(name = "order_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "order_updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "order_deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (orderStatus == null) {
            orderStatus = "PENDING_PAYMENT";
        }
        if (paymentStatus == null) {
            paymentStatus = "UNPAID";
        }
        if (orderDiscountAmount == null) {
            orderDiscountAmount = BigDecimal.ZERO;
        }
        if (orderShippingFee == null) {
            orderShippingFee = BigDecimal.ZERO;
        }
        // Tính total amount
        if (orderSubtotalAmount != null && orderDiscountAmount != null && orderShippingFee != null) {
            orderTotalAmount = orderSubtotalAmount.subtract(orderDiscountAmount).add(orderShippingFee);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (orderSubtotalAmount != null && orderDiscountAmount != null && orderShippingFee != null) {
            orderTotalAmount = orderSubtotalAmount.subtract(orderDiscountAmount).add(orderShippingFee);
        }
    }
}