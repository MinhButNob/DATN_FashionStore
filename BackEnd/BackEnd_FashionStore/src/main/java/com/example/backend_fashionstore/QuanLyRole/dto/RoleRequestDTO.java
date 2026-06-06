// RoleRequestDTO.java
package com.example.backend_fashionstore.QuanLyRole.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class RoleRequestDTO {
    @NotBlank(message = "Tên role không được để trống")
    private String roleName;

    private String status;  // ✅ Đổi từ Boolean sang String (ACTIVE, INACTIVE)
}