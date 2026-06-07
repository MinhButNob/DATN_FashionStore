package com.example.backend_fashionstore.quanlyNhanVien.dto;

import com.example.backend_fashionstore.entity.Employee;
import com.example.backend_fashionstore.enums.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        response.setEmployeeId(employee.getEmployeeId());

        // ✅ Lấy thông tin từ User
        if (employee.getUser() != null) {
            response.setUserId(employee.getUser().getUserId());
            response.setUsername(employee.getUser().getUsername());
            response.setEmail(employee.getUser().getEmail());

            UserStatus userStatus = employee.getUser().getStatus();
            response.setUserStatus(userStatus != null ? userStatus.name() : "ACTIVE");
            response.setIsLocked(userStatus == UserStatus.BANNED);
            response.setIsActive(userStatus == UserStatus.ACTIVE);
        }

        response.setEmployeeFullname(employee.getEmployeeFullname());
        response.setEmployeeGender(employee.getEmployeeGender());
        response.setEmployeePhone(employee.getEmployeePhone());
        response.setEmployeeEmail(employee.getEmployeeEmail());
        response.setEmployeeStatus(employee.getEmployeeStatus());
        response.setEmployeePosition(employee.getEmployeePosition());
        response.setCreatedAt(employee.getCreatedAt());
        response.setUpdatedAt(employee.getUpdatedAt());

        return response;
    }
}