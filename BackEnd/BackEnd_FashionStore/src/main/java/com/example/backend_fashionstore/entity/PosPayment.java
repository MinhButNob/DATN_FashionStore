package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "pos_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pos_payment_id")
    private Integer posPaymentId;

    @Column(name = "pos_payment_order_id", nullable = false)
    private Integer posPaymentOrderId;

    @Column(name = "pos_payment_method_id", nullable = false)
    private Integer posPaymentMethodId;

    @Column(name = "pos_payment_amount", nullable = false)
    private BigDecimal posPaymentAmount;

    @Column(name = "pos_payment_reference_number", length = 100)
    private String posPaymentReferenceNumber;

    @Column(name = "pos_payment_status", nullable = false, length = 20)
    private String posPaymentStatus;

    @Column(name = "pos_payment_processed_by")
    private Integer posPaymentProcessedBy;

    @Column(name = "pos_payment_processed_at", nullable = false)
    private LocalDateTime posPaymentProcessedAt;

    @Column(name = "pos_payment_notes")
    private String posPaymentNotes;

    @CreationTimestamp
    @Column(name = "pos_payment_created_at", updatable = false)
    private LocalDateTime posPaymentCreatedAt;

    @UpdateTimestamp
    @Column(name = "pos_payment_updated_at")
    private LocalDateTime posPaymentUpdatedAt;

    @Column(name = "pos_payment_deleted_at")
    private LocalDateTime posPaymentDeletedAt;
}

