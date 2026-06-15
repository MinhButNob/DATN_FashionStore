package com.example.backend_fashionstore.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "pos_cash_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosCashTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cash_transaction_id")
    private Integer cashTransactionId;

    @Column(name = "cash_transaction_session_id", nullable = false)
    private Integer cashTransactionSessionId;

    @Column(name = "cash_transaction_type", nullable = false, length = 20)
    private String cashTransactionType;

    @Column(name = "cash_transaction_amount", nullable = false)
    private BigDecimal cashTransactionAmount;

    @Column(name = "cash_transaction_reason", nullable = false)
    private String cashTransactionReason;

    @Column(name = "cash_transaction_employee_id", nullable = false)
    private Integer cashTransactionEmployeeId;

    @Column(name = "cash_transaction_before_balance", nullable = false)
    private BigDecimal cashTransactionBeforeBalance;

    @Column(name = "cash_transaction_after_balance", nullable = false)
    private BigDecimal cashTransactionAfterBalance;

    @CreationTimestamp
    @Column(name = "cash_transaction_created_at", updatable = false)
    private LocalDateTime cashTransactionCreatedAt;

    @Column(name = "cash_transaction_deleted_at")
    private LocalDateTime cashTransactionDeletedAt;
}
