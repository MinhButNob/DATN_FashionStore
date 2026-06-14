// entity/PosOrder.java
package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pos_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pos_order_id")
    private Integer posOrderId;

    @Column(name = "pos_order_session_id", nullable = false)
    private Integer posOrderSessionId;

    @Column(name = "pos_order_customer_id")
    private Integer posOrderCustomerId;

    @Column(name = "pos_order_invoice_number", nullable = false, unique = true, length = 50)
    private String posOrderInvoiceNumber;

    @Column(name = "pos_order_type", nullable = false, length = 20)
    private String posOrderType;

    @Column(name = "pos_order_subtotal", nullable = false)
    private BigDecimal posOrderSubtotal;

    @Column(name = "pos_order_discount_amount", nullable = false)
    private BigDecimal posOrderDiscountAmount;

    @Column(name = "pos_order_tax_amount", nullable = false)
    private BigDecimal posOrderTaxAmount;

    @Column(name = "pos_order_total_amount", insertable = false, updatable = false)
    private BigDecimal posOrderTotalAmount;

    @Column(name = "pos_order_paid_amount", nullable = false)
    private BigDecimal posOrderPaidAmount;

    @Column(name = "pos_order_change_amount", insertable = false, updatable = false)
    private BigDecimal posOrderChangeAmount;

    @Column(name = "pos_order_status", nullable = false, length = 20)
    private String posOrderStatus;

    @Column(name = "pos_order_payment_status", nullable = false, length = 20)
    private String posOrderPaymentStatus;

    @Column(name = "pos_order_customer_name", length = 100)
    private String posOrderCustomerName;

    @Column(name = "pos_order_customer_phone", length = 20)
    private String posOrderCustomerPhone;

    @Column(name = "pos_order_notes")
    private String posOrderNotes;

    @CreationTimestamp
    @Column(name = "pos_order_created_at", updatable = false)
    private LocalDateTime posOrderCreatedAt;

    @UpdateTimestamp
    @Column(name = "pos_order_updated_at")
    private LocalDateTime posOrderUpdatedAt;

    @Column(name = "pos_order_deleted_at")
    private LocalDateTime posOrderDeletedAt;
}