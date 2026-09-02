package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "damaged_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DamagedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "damaged_product_id")
    private Integer damagedProductId;

    @Column(name = "product_detail_id", nullable = false)
    private Integer productDetailId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 15, scale = 0)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 15, scale = 0)
    private BigDecimal totalAmount;

    @Column(name = "return_exchange_id")
    private Integer returnExchangeId;

    @Column(name = "return_exchange_item_id")
    private Integer returnExchangeItemId;

    @Column(name = "order_id")
    private Integer orderId;

    // ✅ THÊM: ID của item trong đơn hàng mới (khi đổi sản phẩm)
    @Column(name = "exchange_item_id")
    private Integer exchangeItemId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, PROCESSED, DISCARDED, REPAIRED

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by")
    private Integer processedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
        if (unitPrice == null) {
            unitPrice = BigDecimal.ZERO;
        }
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
    }
}