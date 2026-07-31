package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PosOrderRequest {
    private Integer posSessionId;
    private Integer posOrderCustomerId;
    private String posOrderCustomerName;   // Khách vãng lai
    private String posOrderCustomerPhone;  // Khách vãng lai
    private String posOrderType;           // AT_COUNTER, TAKEAWAY, DELIVERY
    private BigDecimal posOrderDiscountAmount;
    private String voucherCode;
    private BigDecimal posOrderTaxAmount;
    private String posOrderNotes;
    private List<PosOrderItemRequest> items;
    private List<PosPaymentRequest> payments;
}
