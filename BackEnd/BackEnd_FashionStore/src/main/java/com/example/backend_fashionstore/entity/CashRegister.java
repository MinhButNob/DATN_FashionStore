package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

// =====================================================
// 1. CASH_REGISTERS (Quầy thu ngân)
// =====================================================
@Entity
@Table(name = "cash_registers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cash_register_id")
    private Integer cashRegisterId;

    @Column(name = "cash_register_name", nullable = false, length = 100)
    private String cashRegisterName;

    @Column(name = "cash_register_location", length = 255)
    private String cashRegisterLocation;

    @Column(name = "cash_register_status", nullable = false, length = 20)
    private String cashRegisterStatus;

    @Column(name = "cash_register_opened_at")
    private LocalDateTime cashRegisterOpenedAt;

    @Column(name = "cash_register_closed_at")
    private LocalDateTime cashRegisterClosedAt;

    @Column(name = "cash_register_opening_balance", nullable = false)
    private BigDecimal cashRegisterOpeningBalance;

    @Column(name = "cash_register_closing_balance")
    private BigDecimal cashRegisterClosingBalance;

    @Column(name = "cash_register_expected_balance")
    private BigDecimal cashRegisterExpectedBalance;

    @Column(name = "cash_register_difference")
    private BigDecimal cashRegisterDifference;

    @Column(name = "cash_register_opened_by")
    private Integer cashRegisterOpenedBy;

    @Column(name = "cash_register_closed_by")
    private Integer cashRegisterClosedBy;

    @CreationTimestamp
    @Column(name = "cash_register_created_at", updatable = false)
    private LocalDateTime cashRegisterCreatedAt;

    @UpdateTimestamp
    @Column(name = "cash_register_updated_at")
    private LocalDateTime cashRegisterUpdatedAt;

    @Column(name = "cash_register_deleted_at")
    private LocalDateTime cashRegisterDeletedAt;
}