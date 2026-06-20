package com.example.backend_fashionstore.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_detail_id")
    private Long reportDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_detail_report_id", nullable = false)
    private StatisticalReport report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_detail_product_id")
    private Product product;

    @Column(name = "quantity_sold", nullable = false)
    private Integer quantitySold;

    @Column(name = "revenue", nullable = false, precision = 15, scale = 2)
    private BigDecimal revenue;

    @Column(name = "cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal cost;

    @Column(name = "profit", nullable = false, precision = 15, scale = 2)
    private BigDecimal profit;

    @Column(name = "report_detail_created_at", nullable = false, updatable = false)
    private LocalDateTime reportDetailCreatedAt;

    @Column(name = "report_detail_updated_at", nullable = false)
    private LocalDateTime reportDetailUpdatedAt;

    @Column(name = "report_detail_deleted_at")
    private LocalDateTime reportDetailDeletedAt;

    @PrePersist
    protected void onCreate() {
        reportDetailCreatedAt = LocalDateTime.now();
        reportDetailUpdatedAt = LocalDateTime.now();
        if (quantitySold == null) quantitySold = 0;
        if (revenue == null) revenue = BigDecimal.ZERO;
        if (cost == null) cost = BigDecimal.ZERO;
        if (profit == null) profit = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        reportDetailUpdatedAt = LocalDateTime.now();
    }
}