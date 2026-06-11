package com.example.backend_fashionstore.QuanLyDanhMuc.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponseDTO {
    private Integer categoryId;
    private String categoryName;
    private String categoryDescription;
    private String status;
}