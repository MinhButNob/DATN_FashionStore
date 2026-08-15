package com.example.backend_fashionstore.QuanLyDiaChi.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddressRequestDTO {
    private Integer provinceId;
    private Integer wardId;
    private String provinceCode;
    private String provinceName;
    private String districtCode;
    private String districtName;
    private String wardCode;
    private String wardName;
    private String streetAddress;
    private String addressDetail;
    private String fullName;
    private String phone;
    private Boolean isDefault;
}
