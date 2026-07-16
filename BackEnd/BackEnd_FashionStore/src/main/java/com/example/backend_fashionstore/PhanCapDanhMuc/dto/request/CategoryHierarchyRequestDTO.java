package com.example.backend_fashionstore.PhanCapDanhMuc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryHierarchyRequestDTO {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 20, message = "Tên danh mục không được vượt quá 20 ký tự")
    private String categoryName;

    @Size(max = 20, message = "Mô tả không được vượt quá 20 ký tự")
    private String categoryDescription;

    private String status;

    // null = danh mục cấp 1 (không liên quan / không có cha)
    // có giá trị = danh mục cấp 2, là con của danh mục có ID này
    private Integer parentCategoryId;
}