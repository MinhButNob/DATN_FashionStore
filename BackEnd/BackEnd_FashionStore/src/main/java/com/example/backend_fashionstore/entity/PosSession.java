package com.example.backend_fashionstore.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pos_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pos_session_id")
    private Integer posSessionId;

    @Column(name = "pos_session_cash_register_id", nullable = false)
    private Integer posSessionCashRegisterId;

    @Column(name = "pos_session_employee_id", nullable = false)
    private Integer posSessionEmployeeId;

    @Column(name = "pos_session_start_time", nullable = false)
    private LocalDateTime posSessionStartTime;

    @Column(name = "pos_session_end_time")
    private LocalDateTime posSessionEndTime;

    @Column(name = "pos_session_status", nullable = false, length = 20)
    private String posSessionStatus;

    @Column(name = "pos_session_start_balance", nullable = false)
    private BigDecimal posSessionStartBalance;

    @Column(name = "pos_session_end_balance")
    private BigDecimal posSessionEndBalance;

    @Column(name = "pos_session_cash_sales", nullable = false)
    private BigDecimal posSessionCashSales;

    @Column(name = "pos_session_card_sales", nullable = false)
    private BigDecimal posSessionCardSales;

    @Column(name = "pos_session_transfer_sales", nullable = false)
    private BigDecimal posSessionTransferSales;

    @Column(name = "pos_session_total_sales", insertable = false, updatable = false)
    private BigDecimal posSessionTotalSales;

    @Column(name = "pos_session_notes")
    private String posSessionNotes;

    @CreationTimestamp
    @Column(name = "pos_session_created_at", updatable = false)
    private LocalDateTime posSessionCreatedAt;

    @UpdateTimestamp
    @Column(name = "pos_session_updated_at")
    private LocalDateTime posSessionUpdatedAt;

    @Column(name = "pos_session_deleted_at")
    private LocalDateTime posSessionDeletedAt;
}