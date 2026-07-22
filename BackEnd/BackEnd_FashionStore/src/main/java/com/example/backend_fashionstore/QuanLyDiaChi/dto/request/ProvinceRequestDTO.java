package com.example.backend_fashionstore.QuanLyDiaChi.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProvinceRequestDTO {
    private String provinceName;
    private String provinceCode;
    private String provinceType;
    private Integer displayOrder;
}