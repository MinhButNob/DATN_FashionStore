package com.example.backend_fashionstore.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Tên không được để trống")
    @Size(min = 2, message = "Tên phải có ít nhất 2 ký tự")
    private String name;

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Số điện thoại không được để trống")
    @jakarta.validation.constraints.Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ (phải gồm 10 chữ số và bắt đầu bằng 0)")
    private String phone;
}

