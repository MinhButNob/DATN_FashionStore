package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PosOrderResponse {
    private Integer posOrderId;
    private Integer posOrderSessionId;
    private Integer posOrderCustomerId;
    private String posOrderInvoiceNumber;
    private String posOrderType;
    private BigDecimal posOrderSubtotal;
    private BigDecimal posOrderDiscountAmount;
    private BigDecimal posOrderTaxAmount;
    private BigDecimal posOrderTotalAmount;
    private BigDecimal posOrderPaidAmount;
    private BigDecimal posOrderChangeAmount;
    private String posOrderStatus;
    private String posOrderPaymentStatus;
    private String posOrderCustomerName;
    private String posOrderCustomerPhone;
    private String posOrderNotes;
    private LocalDateTime posOrderCreatedAt;
    private Integer exchangeOrderId;
    private Integer parentOrderId;
    private List<PosOrderItemResponse> items;
    private List<PosPaymentResponse> payments;

    private Integer voucherId;
    private String voucherCode;
    private BigDecimal voucherDiscountAmount;
    private BigDecimal voucherDiscountPercent;
    private BigDecimal voucherMaxDiscountAmount; // ✅ Đã có
}