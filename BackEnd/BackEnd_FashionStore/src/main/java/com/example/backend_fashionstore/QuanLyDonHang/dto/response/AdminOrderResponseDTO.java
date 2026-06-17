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
public class AdminOrderResponseDTO {
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private AdminCustomerInfoDTO customer;
    private AdminShippingAddressDTO shippingAddress;
    private AdminVoucherInfoDTO voucher;
    private List<AdminOrderItemResponseDTO> items;
}