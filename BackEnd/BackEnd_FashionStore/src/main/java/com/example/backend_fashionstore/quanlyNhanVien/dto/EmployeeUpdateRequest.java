package com.example.backend_fashionstore.quanlyNhanVien.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeUpdateRequest extends EmployeeRequest {
    // Username và password là tùy chọn khi cập nhật
    private String username;
    private String password;

    // ✅ THÊM: cập nhật role
    private String roleName;
}