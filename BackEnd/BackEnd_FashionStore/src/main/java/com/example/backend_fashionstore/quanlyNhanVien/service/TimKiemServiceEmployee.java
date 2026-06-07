package com.example.backend_fashionstore.quanlyNhanVien.service;

import com.example.backend_fashionstore.entity.Employee;
import com.example.backend_fashionstore.enums.UserStatus;
import com.example.backend_fashionstore.quanlyNhanVien.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimKiemServiceEmployee {

    private final EmployeeRepository employeeRepository;

    public Employee findById(Integer id) {
        Employee employee = employeeRepository.findByEmployeeId(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        return employee;
    }

    public List<Employee> findAll() {
        // ✅ Lấy tất cả nhân viên bao gồm đã xóa
        return employeeRepository.findAll();
    }

    public long countActiveEmployees() {
        return employeeRepository.countActiveEmployees();
    }

    public long countTotalEmployees() {
        return employeeRepository.countTotalEmployees();
    }

    // ✅ Thống kê theo user status
    public long countByUserStatus(UserStatus status) {
        return employeeRepository.countByUserStatus(status);
    }
}