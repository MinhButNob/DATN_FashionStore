package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PosReturnExchangeResponse {
    private Integer returnExchangeId;
    private Integer orderId;
    private String orderInvoiceNumber;
    private String customerName;
    private String customerPhone;
    private String type;               // RETURN hoặc EXCHANGE
    private BigDecimal totalRefundAmount;
    private BigDecimal additionalPayment;
    private String reason;
    private String status;             // PENDING, COMPLETED, CANCELLED
    private String referenceCode;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;   // ✅ Thêm thời gian cập nhật
    private Integer employeeId;        // ✅ Thêm ID nhân viên xử lý
    private String employeeName;       // ✅ Thêm tên nhân viên xử lý
    private List<ReturnExchangeItemResponse> items;
}