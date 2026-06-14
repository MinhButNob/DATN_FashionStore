package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PosReceiptLogResponse {
    private Integer receiptLogId;
    private Integer receiptLogOrderId;
    private LocalDateTime receiptLogPrintedAt;
    private Integer receiptLogPrintedBy;
    private Integer receiptLogCopies;
    private String receiptLogPrinterName;
    private String receiptLogStatus;
    private String receiptLogErrorMessage;
}
