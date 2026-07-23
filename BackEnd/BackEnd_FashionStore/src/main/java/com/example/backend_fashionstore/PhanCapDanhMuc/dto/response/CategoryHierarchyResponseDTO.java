package com.example.backend_fashionstore.PhanCapDanhMuc.dto.response;

import lombok.Builder;
import lombok.Data;

// Ghi đè file: src/main/java/com/example/backend_fashionstore/PhanCapDanhMuc/dto/response/CategoryHierarchyResponseDTO.java
@Data
@Builder
public class CategoryHierarchyResponseDTO {
    private Integer categoryId;
    private String categoryName;
    private String categoryDescription;
    private String status;
    private Integer parentCategoryId;

    // (MỚI) - tên field khớp với FE (categoryHierarchyStores.js: item.categoryIsLocked)
    private Boolean categoryIsLocked;
}