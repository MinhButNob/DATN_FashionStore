package com.example.backend_fashionstore.PhanCapDanhMuc.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryHierarchyResponseDTO {
    private Integer categoryId;
    private String categoryName;
    private String categoryDescription;
    private String status;
    private Integer parentCategoryId;
}