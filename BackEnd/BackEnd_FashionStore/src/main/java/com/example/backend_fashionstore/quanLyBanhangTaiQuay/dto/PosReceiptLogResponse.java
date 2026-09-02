package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    // ✅ Thêm thông tin chi tiết đơn hàng
    private String invoiceNumber;
    private String customerName;
    private String customerPhone;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal changeAmount;
    private String paymentStatus;
    private LocalDateTime orderCreatedAt;

    // ✅ Danh sách sản phẩm trong hóa đơn
    private List<ReceiptItemResponse> items;
}