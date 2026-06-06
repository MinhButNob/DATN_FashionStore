// RoleResponseDTO.java
package com.example.backend_fashionstore.QuanLyRole.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleResponseDTO {
    private Integer roleId;
    private String roleName;
    private String status;  // ✅ Đổi từ Boolean sang String
}