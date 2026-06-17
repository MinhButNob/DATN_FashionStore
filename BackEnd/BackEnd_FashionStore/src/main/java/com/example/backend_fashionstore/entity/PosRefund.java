package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "pos_refunds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pos_refund_id")
    private Integer posRefundId;

    @Column(name = "pos_refund_order_id", nullable = false)
    private Integer posRefundOrderId;

    @Column(name = "pos_refund_amount", nullable = false)
    private BigDecimal posRefundAmount;

    @Column(name = "pos_refund_reason", nullable = false)
    private String posRefundReason;

    @Column(name = "pos_refund_method_id", nullable = false)
    private Integer posRefundMethodId;

    @Column(name = "pos_refund_employee_id", nullable = false)
    private Integer posRefundEmployeeId;

    @Column(name = "pos_refund_status", nullable = false, length = 20)
    private String posRefundStatus;

    @Column(name = "pos_refund_approved_by")
    private Integer posRefundApprovedBy;

    @Column(name = "pos_refund_approved_at")
    private LocalDateTime posRefundApprovedAt;

    @Column(name = "pos_refund_notes")
    private String posRefundNotes;

    @CreationTimestamp
    @Column(name = "pos_refund_created_at", updatable = false)
    private LocalDateTime posRefundCreatedAt;

    @UpdateTimestamp
    @Column(name = "pos_refund_updated_at")
    private LocalDateTime posRefundUpdatedAt;

    @Column(name = "pos_refund_deleted_at")
    private LocalDateTime posRefundDeletedAt;
}
