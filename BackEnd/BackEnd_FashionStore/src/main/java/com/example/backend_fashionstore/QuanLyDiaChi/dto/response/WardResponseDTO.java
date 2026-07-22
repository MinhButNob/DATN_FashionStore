package com.example.backend_fashionstore.QuanLyDiaChi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WardResponseDTO {
    private Integer wardId;
    private String wardName;
    private String wardCode;
    private String wardType;
    private Integer displayOrder;
    private String status;
    private Integer provinceId;
    private String provinceName;
}