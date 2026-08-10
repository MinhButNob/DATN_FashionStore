package com.example.backend_fashionstore.customer.order.dto.response;

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
public class OrderResponseDTO {
    private Long orderId;
    private String orderCode;
    private LocalDateTime orderDate;
    private String orderStatus;
    private String paymentStatus;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;  // ✅ THÊM
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private BigDecimal refundAmount;
    private String notes;
    private LocalDateTime createdAt;

    private CustomerInfoDTO customer;
    private ShippingAddressDTO shippingAddress;
    private VoucherInfoDTO voucher;  // ✅ THÊM - đã có
    private List<OrderItemResponseDTO> items;
}