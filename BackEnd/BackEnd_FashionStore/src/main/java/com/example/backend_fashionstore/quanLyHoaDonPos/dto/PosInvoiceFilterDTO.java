package com.example.backend_fashionstore.quanLyHoaDonPos.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PosInvoiceFilterDTO {
    private String invoiceNumber;
    private String customerName;
    private String customerPhone;
    private String orderStatus;
    private String paymentStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer cashierId;
    private Integer page = 0;
    private Integer size = 20;
//    private String sortBy = "createdAt";
private String sortBy = "posOrderCreatedAt";
    private Integer posSessionId;
    private String sortDir = "DESC";
}