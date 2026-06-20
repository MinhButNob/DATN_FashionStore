package com.example.backend_fashionstore.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "statistical_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticalReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_employee_id")
    private Employee employee;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "file_path", length = 255)
    private String filePath;

    @Column(name = "note")
    private String note;

    @Column(name = "report_created_at", nullable = false, updatable = false)
    private LocalDateTime reportCreatedAt;

    @Column(name = "report_updated_at", nullable = false)
    private LocalDateTime reportUpdatedAt;

    @Column(name = "report_deleted_at")
    private LocalDateTime reportDeletedAt;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        reportCreatedAt = LocalDateTime.now();
        reportUpdatedAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        reportUpdatedAt = LocalDateTime.now();
    }
}