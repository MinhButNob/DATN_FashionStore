// DamagedProductDTO.java
package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DamagedProductDTO {
    private Integer damagedProductId;
    private Integer productDetailId;
    private String productName;
    private String colorName;
    private String sizeName;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Integer returnExchangeId;
    private String returnExchangeCode;
    private Integer orderId;
    private String orderInvoiceNumber;
    private String reason;
    private String status;
    private String statusText;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String processedByName;
}