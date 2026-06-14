package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "pos_holds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pos_hold_id")
    private Integer posHoldId;

    @Column(name = "pos_hold_session_id", nullable = false)
    private Integer posHoldSessionId;

    @Column(name = "pos_hold_customer_id")
    private Integer posHoldCustomerId;

    @Column(name = "pos_hold_reference_code", nullable = false, unique = true, length = 50)
    private String posHoldReferenceCode;

    @Column(columnDefinition = "jsonb")
    private String posHoldItemsData;

    @Column(name = "pos_hold_subtotal", nullable = false)
    private BigDecimal posHoldSubtotal;

    @Column(name = "pos_hold_discount_amount", nullable = false)
    private BigDecimal posHoldDiscountAmount;

    @Column(name = "pos_hold_total_amount", nullable = false)
    private BigDecimal posHoldTotalAmount;

    @Column(name = "pos_hold_notes")
    private String posHoldNotes;

    @Column(name = "pos_hold_expiry_time", nullable = false)
    private LocalDateTime posHoldExpiryTime;

    @Column(name = "pos_hold_status", nullable = false, length = 20)
    private String posHoldStatus;

    @CreationTimestamp
    @Column(name = "pos_hold_created_at", updatable = false)
    private LocalDateTime posHoldCreatedAt;

    @UpdateTimestamp
    @Column(name = "pos_hold_updated_at")
    private LocalDateTime posHoldUpdatedAt;

    @Column(name = "pos_hold_deleted_at")
    private LocalDateTime posHoldDeletedAt;
}