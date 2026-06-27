// InvoiceDTO.java
package com.example.backend_fashionstore.QuanLyDonHang.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {
    private Long orderId;
    private String orderCode;
    private LocalDateTime orderDate;
    private String orderStatus;
    private String paymentStatus;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String notes;

    // Thông tin khách hàng
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    // Thông tin địa chỉ giao hàng
    private String shippingFullName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingCity;
    private String shippingDistrict;
    private String shippingWard;

    // Thông tin voucher
    private String voucherCode;
    private BigDecimal voucherDiscountAmount;

    // Danh sách sản phẩm
    private List<InvoiceItemDTO> items;
}