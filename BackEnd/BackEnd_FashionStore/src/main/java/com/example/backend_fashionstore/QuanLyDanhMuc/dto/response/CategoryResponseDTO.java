package com.example.backend_fashionstore.QuanLyDanhMuc.dto.response;

import lombok.Builder;
import lombok.Data;

// Ghi đè file: src/main/java/com/example/backend_fashionstore/QuanLyDanhMuc/dto/response/CategoryResponseDTO.java
@Data
@Builder
public class CategoryResponseDTO {
    private Integer categoryId;
    private String categoryName;
    private String categoryDescription;
    private String status;

    // trả kèm parentCategoryId để FE biết danh mục này có đang
    // thuộc 1 trong 3 danh mục lớn hay không (null = độc lập).
    private Integer parentCategoryId;

    // (MỚI) FE (categoryHierarchyStores.js) đọc đúng tên field này để hiện
    // badge "Mặc định" / khóa nút sửa-xóa, thay vì so sánh cứng theo tên category.
    private Boolean categoryIsLocked;
}