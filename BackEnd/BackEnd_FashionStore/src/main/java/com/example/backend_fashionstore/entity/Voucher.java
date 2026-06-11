package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_id")
    private Integer voucherId;

    @Column(name = "voucher_code", nullable = false, length = 20)
    private String voucherCode;

    @Column(name = "voucher_discount_amount", precision = 15, scale = 2)
    private BigDecimal voucherDiscountAmount;

    @Column(name = "voucher_discount_percent", precision = 5, scale = 2)
    private BigDecimal voucherDiscountPercent;

    @Column(name = "voucher_start_date", nullable = false)
    private LocalDateTime voucherStartDate;

    @Column(name = "voucher_end_date", nullable = false)
    private LocalDateTime voucherEndDate;

    @Column(name = "voucher_max_usage")
    private Integer voucherMaxUsage;

    @Column(name = "voucher_used_count", nullable = false)
    private Integer voucherUsedCount = 0;

    @Column(name = "voucher_min_order_value", nullable = false)
    private BigDecimal voucherMinOrderValue = BigDecimal.ZERO;

    @Column(name = "voucher_status", nullable = false, length = 20)
    private String voucherStatus = "ACTIVE";

    @Column(name = "voucher_created_at", nullable = false)
    private LocalDateTime voucherCreatedAt;

    @Column(name = "voucher_updated_at", nullable = false)
    private LocalDateTime voucherUpdatedAt;

    @Column(name = "voucher_deleted_at")
    private LocalDateTime voucherDeletedAt;

    @PrePersist
    public void prePersist() {
        voucherCreatedAt = LocalDateTime.now();
        voucherUpdatedAt = LocalDateTime.now();
        if (voucherUsedCount == null) voucherUsedCount = 0;
        if (voucherMinOrderValue == null) voucherMinOrderValue = BigDecimal.ZERO;
    }

    @PreUpdate
    public void preUpdate() {
        voucherUpdatedAt = LocalDateTime.now();
    }
}