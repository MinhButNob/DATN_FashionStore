package com.example.backend_fashionstore.PhanCapDanhMuc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// Ghi đè file: src/main/java/com/example/backend_fashionstore/PhanCapDanhMuc/dto/response/CategoryTreeResponseDTO.java
@Data
@Builder
public class CategoryTreeResponseDTO {
    private Integer categoryId;
    private String categoryName;
    private String categoryDescription;
    private String status;

    // (MỚI) - tên field khớp với FE (categoryHierarchyStores.js: item.categoryIsLocked)
    private Boolean categoryIsLocked;

    private List<CategoryHierarchyResponseDTO> children;
}