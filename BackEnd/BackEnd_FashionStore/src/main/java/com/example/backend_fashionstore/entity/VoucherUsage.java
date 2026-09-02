package com.example.backend_fashionstore.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "voucher_usage")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_usage_id")
    private Long voucherUsageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "discount_applied", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountApplied;

    @PrePersist
    protected void onCreate() {
        usedAt = LocalDateTime.now();
        if (discountApplied == null) {
            discountApplied = BigDecimal.ZERO;
        }
    }
}