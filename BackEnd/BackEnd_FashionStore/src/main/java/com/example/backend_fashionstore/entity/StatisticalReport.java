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

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "report_type", nullable = false)
    private String reportType;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "status")
    private String status;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "note")
    private String note;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
    }
}