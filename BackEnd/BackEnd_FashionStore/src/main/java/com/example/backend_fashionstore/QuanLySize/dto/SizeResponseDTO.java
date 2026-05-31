package com.example.backend_fashionstore.QuanLySize.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SizeResponseDTO {

    private Integer sizeId;

    private String sizeCode;

    private String sizeName;

    private String description;

    private Boolean status;
}