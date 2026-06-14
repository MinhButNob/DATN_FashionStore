package com.example.backend_fashionstore.quanLyDonHang.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UnifiedOrderResponse {
    private String orderId;           // "ONLINE-123" hoặc "POS-456"
    private String orderType;         // "ONLINE" hoặc "POS"
    private String invoiceNumber;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal changeAmount;
    private String orderStatus;
    private String paymentStatus;
    private String paymentMethod;
    private String notes;
    private LocalDateTime createdAt;
    private List<UnifiedOrderItemResponse> items;
    private List<UnifiedPaymentResponse> payments;
}

@Data
class UnifiedOrderItemResponse {
    private Integer productDetailId;
    private String productName;
    private String colorName;
    private String sizeName;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal total;
}

@Data
class UnifiedPaymentResponse {
    private Integer paymentMethodId;
    private String paymentMethodName;
    private BigDecimal amount;
    private String referenceNumber;
    private LocalDateTime processedAt;
}