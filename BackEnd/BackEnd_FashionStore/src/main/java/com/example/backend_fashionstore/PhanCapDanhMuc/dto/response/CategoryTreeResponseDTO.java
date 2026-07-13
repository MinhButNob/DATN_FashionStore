package com.example.backend_fashionstore.PhanCapDanhMuc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CategoryTreeResponseDTO {
    private Integer categoryId;
    private String categoryName;
    private String categoryDescription;
    private String status;
    private List<CategoryHierarchyResponseDTO> children;
}