package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "product_stock_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_alert_id")
    private Integer stockAlertId;

    @Column(name = "stock_alert_product_detail_id", nullable = false)
    private Integer stockAlertProductDetailId;

    @Column(name = "stock_alert_threshold", nullable = false)
    private Integer stockAlertThreshold;

    @Column(name = "stock_alert_current_stock", nullable = false)
    private Integer stockAlertCurrentStock;

    @Column(name = "stock_alert_status", nullable = false, length = 20)
    private String stockAlertStatus;

    @CreationTimestamp
    @Column(name = "stock_alert_created_at", updatable = false)
    private LocalDateTime stockAlertCreatedAt;

    @Column(name = "stock_alert_resolved_at")
    private LocalDateTime stockAlertResolvedAt;

    @Column(name = "stock_alert_resolved_by")
    private Integer stockAlertResolvedBy;
}