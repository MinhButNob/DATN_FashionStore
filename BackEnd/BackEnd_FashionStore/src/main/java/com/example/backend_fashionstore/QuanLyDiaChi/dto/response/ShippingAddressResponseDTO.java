package com.example.backend_fashionstore.QuanLyDiaChi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShippingAddressResponseDTO {
    private Long addressId;
    private Integer provinceId;
    private String provinceCode;
    private String provinceName;
    private String districtCode;
    private Integer wardId;
    private String wardName;
    private String districtName;  // ✅ THÊM
    private String wardCode;
    private String streetAddress;
    private String addressDetail;
    private Boolean needsUpdate;
    private String fullAddress;
    private String fullName;
    private String phone;
    private Boolean isDefault;
    private String status;
}
