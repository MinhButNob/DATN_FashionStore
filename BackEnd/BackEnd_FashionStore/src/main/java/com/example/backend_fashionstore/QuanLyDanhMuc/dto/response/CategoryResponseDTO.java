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

    // (Thêm mới) trả kèm parentCategoryId để FE biết danh mục này có đang
    // thuộc 1 trong 3 danh mục lớn hay không (null = độc lập).
    private Integer parentCategoryId;
}