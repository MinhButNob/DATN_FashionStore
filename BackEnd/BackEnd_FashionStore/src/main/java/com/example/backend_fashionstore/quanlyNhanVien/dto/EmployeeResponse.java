package com.example.backend_fashionstore.quanlyNhanVien.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmployeeResponse {
    private Integer employeeId;
    private Integer userId;
    private String employeeFullname;
    private Boolean employeeGender;
    private Integer employeeAge;
    private String employeePhone;
    private String employeeEmail;
    private String employeeStatus;
    private String employeePosition;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Tính trực tiếp, không cần field riêng tránh serialize null
    public String getGenderText() {
        return employeeGender != null ? (employeeGender ? "Nam" : "Nữ") : "";
    }
}