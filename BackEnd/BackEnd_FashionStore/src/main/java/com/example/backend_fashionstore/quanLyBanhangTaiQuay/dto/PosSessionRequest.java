package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PosSessionRequest {
    private Integer cashRegisterId;
    private Integer employeeId;
    private BigDecimal openingBalance;
}