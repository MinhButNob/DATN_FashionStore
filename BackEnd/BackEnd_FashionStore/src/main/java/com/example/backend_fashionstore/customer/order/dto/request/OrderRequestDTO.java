package com.example.backend_fashionstore.customer.order.dto.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {
    private Integer shippingAddressId;
    private Integer voucherId;
    private String paymentMethod;
    private String notes;

    // ✅ THÊM CÁC FIELD MỚI CHO ĐỊA CHỈ GIAO HÀNG TRỰC TIẾP
    private String shippingFullName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingCity;
    private String shippingDistrict;
    private String shippingWard;

    private List<OrderItemRequestDTO> items;
}