package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PosRefundResponse {
    private Integer posRefundId;
    private Integer posRefundOrderId;
    private BigDecimal posRefundAmount;
    private String posRefundReason;
    private Integer posRefundMethodId;
    private Integer posRefundEmployeeId;
    private String posRefundStatus;
    private LocalDateTime posRefundApprovedAt;
    private LocalDateTime posRefundCreatedAt;
}