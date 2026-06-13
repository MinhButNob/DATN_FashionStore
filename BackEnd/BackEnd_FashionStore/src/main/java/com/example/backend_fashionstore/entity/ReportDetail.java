package com.example.backend_fashionstore.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.math.BigDecimal;

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
    @JoinColumn(name = "report_id", nullable = false)
    private StatisticalReport report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "quantity_sold")
    private Integer quantitySold;

    @Column(name = "revenue", precision = 15, scale = 2)
    private BigDecimal revenue;

    @Column(name = "cost", precision = 15, scale = 2)
    private BigDecimal cost;

    @Column(name = "profit", precision = 15, scale = 2)
    private BigDecimal profit;

    @PrePersist
    protected void onCreate() {
        if (quantitySold == null) quantitySold = 0;
        if (revenue == null) revenue = BigDecimal.ZERO;
        if (cost == null) cost = BigDecimal.ZERO;
        if (profit == null) profit = BigDecimal.ZERO;
    }
}