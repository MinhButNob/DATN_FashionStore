package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PosSessionResponse {
    private Integer posSessionId;
    private Integer cashRegisterId;
    private String cashRegisterName;  // ✅ Thêm tên quầy
    private Integer employeeId;
    private String employeeName;      // ✅ Thêm tên nhân viên
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private BigDecimal startBalance;
    private BigDecimal endBalance;
    private BigDecimal cashSales;
    private BigDecimal cardSales;
    private BigDecimal transferSales;
    private BigDecimal totalSales;
    private String notes;
}