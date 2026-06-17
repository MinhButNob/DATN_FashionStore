package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PosCashTransactionRequest {
    private Integer sessionId;
    private String type;      // WITHDRAW, DEPOSIT
    private BigDecimal amount;
    private String reason;
}