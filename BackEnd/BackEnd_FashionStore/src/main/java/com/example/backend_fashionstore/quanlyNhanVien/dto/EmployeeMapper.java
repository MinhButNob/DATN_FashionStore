package com.example.backend_fashionstore.quanlyNhanVien.dto;

import com.example.backend_fashionstore.entity.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        response.setEmployeeId(employee.getEmployeeId());
        response.setUserId(employee.getUserId());
        response.setEmployeeFullname(employee.getEmployeeFullname());
        response.setEmployeeGender(employee.getEmployeeGender());
        response.setEmployeeAge(employee.getEmployeeAge());
        response.setEmployeePhone(employee.getEmployeePhone());
        response.setEmployeeEmail(employee.getEmployeeEmail());
        response.setEmployeeStatus(employee.getEmployeeStatus());
        response.setEmployeePosition(employee.getEmployeePosition());
        response.setCreatedAt(employee.getCreatedAt());
        response.setUpdatedAt(employee.getUpdatedAt());
        return response;
    }
}