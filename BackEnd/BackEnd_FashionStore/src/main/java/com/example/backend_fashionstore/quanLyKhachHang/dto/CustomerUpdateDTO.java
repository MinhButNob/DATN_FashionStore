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
    @jakarta.validation.constraints.Size(min = 2, message = "Tên khách hàng phải có ít nhất 2 ký tự")
    private String customerName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @jakarta.validation.constraints.Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ (phải gồm 10 chữ số và bắt đầu bằng 0)")
    private String phone;

    private String address;
}