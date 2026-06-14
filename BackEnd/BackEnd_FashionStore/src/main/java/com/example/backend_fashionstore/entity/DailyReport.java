package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "daily_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_report_id")
    private Integer dailyReportId;

    @Column(name = "daily_report_date", nullable = false)
    private LocalDateTime dailyReportDate;

    @Column(name = "daily_report_cash_register_id", nullable = false)
    private Integer dailyReportCashRegisterId;

    @Column(name = "daily_report_opening_balance", nullable = false)
    private BigDecimal dailyReportOpeningBalance;

    @Column(name = "daily_report_closing_balance")
    private BigDecimal dailyReportClosingBalance;

    @Column(name = "daily_report_cash_sales", nullable = false)
    private BigDecimal dailyReportCashSales;

    @Column(name = "daily_report_card_sales", nullable = false)
    private BigDecimal dailyReportCardSales;

    @Column(name = "daily_report_transfer_sales", nullable = false)
    private BigDecimal dailyReportTransferSales;

    @Column(name = "daily_report_total_sales", insertable = false, updatable = false)
    private BigDecimal dailyReportTotalSales;

    @Column(name = "daily_report_total_orders", nullable = false)
    private Integer dailyReportTotalOrders;

    @Column(name = "daily_report_total_customers", nullable = false)
    private Integer dailyReportTotalCustomers;

    @Column(name = "daily_report_avg_order_value")
    private BigDecimal dailyReportAvgOrderValue;

    @Column(name = "daily_report_created_by")
    private Integer dailyReportCreatedBy;

    @Column(name = "daily_report_notes")
    private String dailyReportNotes;

    @CreationTimestamp
    @Column(name = "daily_report_created_at", updatable = false)
    private LocalDateTime dailyReportCreatedAt;

    @UpdateTimestamp
    @Column(name = "daily_report_updated_at")
    private LocalDateTime dailyReportUpdatedAt;

    @Column(name = "daily_report_deleted_at")
    private LocalDateTime dailyReportDeletedAt;
}
