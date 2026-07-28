package com.example.backend_fashionstore.QuanLyDiaChi.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardRequestDTO {
    private Integer provinceId;
    private String wardName;
    private String wardCode;
    private String wardType;
    private Integer displayOrder;
}