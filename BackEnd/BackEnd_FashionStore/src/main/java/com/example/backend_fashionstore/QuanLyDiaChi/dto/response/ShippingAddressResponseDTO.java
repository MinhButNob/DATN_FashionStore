package com.example.backend_fashionstore.QuanLyDiaChi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShippingAddressResponseDTO {
    private Long addressId;
    private Integer provinceId;
    private String provinceName;
    private Integer wardId;
    private String wardName;
    private String districtName;  // ✅ THÊM
    private String streetAddress;
    private String fullAddress;
    private String fullName;
    private String phone;
    private Boolean isDefault;
    private String status;
}