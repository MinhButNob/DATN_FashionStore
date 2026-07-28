package com.example.backend_fashionstore.QuanLyDanhMuc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDTO {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 20, message = "Tên danh mục không được vượt quá 20 ký tự")
    private String categoryName;

    @Size(max = 20, message = "Mô tả không được vượt quá 20 ký tự")
    private String categoryDescription;

    private String status;

    // (Thêm mới - không ảnh hưởng dữ liệu cũ) Cho phép gán trực tiếp vào 1 trong
    // 3 danh mục lớn mặc định (Áo/Quần/Phụ kiện) ngay khi thêm/sửa ở màn Quản lý danh mục.
    // null = danh mục độc lập, không thuộc danh mục lớn nào (giữ nguyên hành vi cũ).
    private Integer parentCategoryId;
}