package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @ManyToOne
    @JoinColumn(name = "order_customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "order_shipping_address_id", nullable = true)
    private ShippingAddress shippingAddress;

    @ManyToOne
    @JoinColumn(name = "order_voucher_id", nullable = true)
    private Voucher voucher;

    @Column(name = "order_cart_id", nullable = true)
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

    // ✅ Generated column - KHÔNG insert hoặc update
    @Column(name = "order_total_amount", insertable = false, updatable = false)
    private BigDecimal orderTotalAmount;

    @Column(name = "order_payment_status", nullable = false)
    private String paymentStatus;

    @Column(name = "order_notes")
    private String notes;

    @CreationTimestamp
    @Column(name = "order_created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "order_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "order_deleted_at")
    private LocalDateTime deletedAt;
}