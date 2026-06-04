package com.example.backend_fashionstore.quanlyNhanVien.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EmployeeRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2-100 ký tự")
    private String employeeFullname;

    @NotNull(message = "Giới tính không được để trống")
    private Boolean employeeGender;

    @NotNull(message = "Tuổi không được để trống")
    @Min(value = 18, message = "Tuổi phải từ 18 trở lên")
    @Max(value = 100, message = "Tuổi không được vượt quá 100")
    private Integer employeeAge;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[1-9][0-9]{8,9})$", message = "Số điện thoại không hợp lệ (VD: 0912345678)")
    private String employeePhone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String employeeEmail;

    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(regexp = "^(ACTIVE|INACTIVE|ON_LEAVE|TERMINATED)$", message = "Trạng thái không hợp lệ")
    private String employeeStatus;

    // ✅ SỬA: bỏ @NotBlank để cho phép FE không truyền, service sẽ set mặc định "Nhân viên"
    @Size(max = 50, message = "Chức vụ không được quá 50 ký tự")
    private String employeePosition;
}