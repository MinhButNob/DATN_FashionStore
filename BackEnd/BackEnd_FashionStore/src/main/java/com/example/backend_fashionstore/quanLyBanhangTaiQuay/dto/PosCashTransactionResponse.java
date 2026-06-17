package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PosCashTransactionResponse {
    private Integer cashTransactionId;
    private Integer sessionId;
    private String type;
    private BigDecimal amount;
    private String reason;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private LocalDateTime createdAt;
}