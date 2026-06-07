package com.example.backend_fashionstore.quanlyNhanVien.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EmployeeResponse {
    private Integer employeeId;
    private Integer userId;
    private String username;           // ✅ THÊM: từ User
    private String email;              // ✅ THÊM: từ User
    private String employeeFullname;
    private Boolean employeeGender;
//    private Integer employeeAge;
    private String employeePhone;
    private String employeeEmail;
    private String employeeStatus;
    private String employeePosition;
    private String userStatus;          // ✅ THÊM: status từ User (ACTIVE, BANNED, DELETED)
    private Boolean isLocked;           // ✅ THÊM: true nếu user.status = BANNED
    private Boolean isActive;           // ✅ THÊM: true nếu user.status = ACTIVE
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getGenderText() {
        return employeeGender != null ? (employeeGender ? "Nam" : "Nữ") : "";
    }
}