package com.example.backend_fashionstore.quanLyKhachHang.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUpdateDTO {

    @NotBlank(message = "Tên khách hàng không được để trống")
    private String customerName;

    private String phone;

    private String address;
}