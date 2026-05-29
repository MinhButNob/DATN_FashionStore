package com.example.backend_fashionstore.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUpdateDTO {

    @NotBlank(message = "Tên khách hàng không được để trống")
    private String customerName;

    private String phone;
    private String address;
}