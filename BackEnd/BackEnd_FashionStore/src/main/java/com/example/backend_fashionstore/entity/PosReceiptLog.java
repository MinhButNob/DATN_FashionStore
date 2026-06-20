package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "pos_receipt_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosReceiptLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_log_id")
    private Integer receiptLogId;

    @Column(name = "receipt_log_order_id", nullable = false)
    private Integer receiptLogOrderId;

    @Column(name = "receipt_log_printed_at", nullable = false)
    private LocalDateTime receiptLogPrintedAt;

    @Column(name = "receipt_log_printed_by", nullable = false)
    private Integer receiptLogPrintedBy;

    @Column(name = "receipt_log_copies", nullable = false)
    private Integer receiptLogCopies;

    @Column(name = "receipt_log_printer_name", length = 100)
    private String receiptLogPrinterName;

    @Column(name = "receipt_log_status", nullable = false, length = 20)
    private String receiptLogStatus;

    @Column(name = "receipt_log_error_message")
    private String receiptLogErrorMessage;

    // Thêm 3 trường audit
    @CreationTimestamp
    @Column(name = "receipt_log_created_at", updatable = false)
    private LocalDateTime receiptLogCreatedAt;

    @UpdateTimestamp
    @Column(name = "receipt_log_updated_at")
    private LocalDateTime receiptLogUpdatedAt;

    @Column(name = "receipt_log_deleted_at")
    private LocalDateTime receiptLogDeletedAt;
}