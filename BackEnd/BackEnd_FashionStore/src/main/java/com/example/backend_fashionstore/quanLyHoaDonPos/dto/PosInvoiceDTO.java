package com.example.backend_fashionstore.quanLyHoaDonPos.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PosInvoiceDTO {
    private Integer posOrderId;
    private String invoiceNumber;
    private String orderType;
    private String customerName;
    private String customerPhone;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal total;
    private BigDecimal paidAmount;
    private BigDecimal changeAmount;
    private String orderStatus;
    private String paymentStatus;
    private LocalDateTime createdAt;
    private String cashierName;
    private Integer cashierId;
    private List<PosInvoiceItemDTO> items;
    private List<PosInvoicePaymentDTO> payments;

    // ✅ Thêm trường voucherCode để hiển thị mã giảm giá
    private String voucherCode;
}