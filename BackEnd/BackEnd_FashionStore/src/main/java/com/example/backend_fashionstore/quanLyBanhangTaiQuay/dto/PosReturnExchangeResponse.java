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
    private String type;
    private BigDecimal totalRefundAmount;
    private BigDecimal additionalPayment;
    private String reason;
    private String status;
    private String referenceCode;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<ReturnExchangeItemResponse> items;
}