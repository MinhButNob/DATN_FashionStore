package com.example.backend_fashionstore.quanlyNhanVien.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeUpdateRequest extends EmployeeRequest {
    // Username và password là tùy chọn khi cập nhật
    private String username;

    // ✅ @Size bỏ qua giá trị null (không đổi mật khẩu), nhưng nếu có nhập thì bắt buộc >= 6 ký tự
    // ⚠️ TRƯỚC ĐÂY không có validation nào ở đây, nên đổi mật khẩu 4 ký tự vẫn được backend chấp nhận
    @Size(min = 6, message = "Mật khẩu phải ít nhất 6 ký tự")
    private String password;

    // ✅ THÊM: cập nhật role
    private String roleName;
}