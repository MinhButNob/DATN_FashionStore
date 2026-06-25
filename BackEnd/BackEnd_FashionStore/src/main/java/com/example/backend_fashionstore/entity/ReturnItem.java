package com.example.backend_fashionstore.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_item_id")
    private Long returnItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_item_return_id", nullable = false)
    private ReturnOrder returnOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_item_order_detail_id", nullable = false)
    private OrderDetail orderDetail;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "reason")
    private String reason;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "return_item_created_at", nullable = false, updatable = false)
    private LocalDateTime returnItemCreatedAt;

    @Column(name = "return_item_updated_at", nullable = false)
    private LocalDateTime returnItemUpdatedAt;

    @Column(name = "return_item_deleted_at")
    private LocalDateTime returnItemDeletedAt;

    @PrePersist
    protected void onCreate() {
        returnItemCreatedAt = LocalDateTime.now();
        returnItemUpdatedAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }

    @PreUpdate
    protected void onUpdate() {
        returnItemUpdatedAt = LocalDateTime.now();
    }
}