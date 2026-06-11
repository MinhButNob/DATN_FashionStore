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
}