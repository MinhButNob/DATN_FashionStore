package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pos_return_exchanges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosReturnExchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_exchange_id")
    private Integer returnExchangeId;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "type", nullable = false, length = 20)
    private String type; // 'RETURN' hoặc 'EXCHANGE'

    @Column(name = "total_refund_amount", nullable = false)
    @Builder.Default
    private BigDecimal totalRefundAmount = BigDecimal.ZERO;

    @Column(name = "additional_payment", nullable = false)
    @Builder.Default
    private BigDecimal additionalPayment = BigDecimal.ZERO;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, COMPLETED, CANCELLED

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(name = "reference_code", nullable = false, unique = true, length = 50)
    private String referenceCode;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ✅ THÊM: ID đơn hàng mới được tạo (khi đổi sản phẩm)
    @Column(name = "exchange_order_id")
    private Integer exchangeOrderId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}