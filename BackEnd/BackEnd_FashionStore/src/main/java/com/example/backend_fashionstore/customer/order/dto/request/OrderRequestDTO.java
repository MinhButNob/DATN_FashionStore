package com.example.backend_fashionstore.customer.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {
    // ✅ shippingAddressId để dùng địa chỉ đã lưu
    private Integer shippingAddressId;

    // ✅ Các field cho địa chỉ tạm
    private String shippingFullName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingCity;
    private String shippingDistrict;
    private String shippingWard;

    // ✅ THÊM provinceId VÀ wardId
    private Integer provinceId;
    private Integer wardId;

    private String notes;
    private List<OrderItemRequestDTO> items;
    private String paymentMethod;
    private Integer voucherId;
}