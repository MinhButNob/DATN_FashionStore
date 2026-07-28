package com.example.backend_fashionstore.QuanLyDiaChi.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ProvinceResponseDTO {
    private Integer provinceId;
    private String provinceName;
    private String provinceCode;
    private String provinceType;
    private Integer displayOrder;
    private String status;
    private List<WardResponseDTO> wards;
}