package com.example.backend_fashionstore.quanlyNhanVien.service;

import com.example.backend_fashionstore.entity.Employee;
import com.example.backend_fashionstore.quanlyNhanVien.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimKiemServiceEmployee {

    private final EmployeeRepository employeeRepository;

    public Employee findById(Integer id) {
        return employeeRepository.findByEmployeeId(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
    }

    public List<Employee> findAll() {
        // Dùng findAll() của JPA, không cần query thủ công
        return employeeRepository.findAll();
    }

    public long countActiveEmployees() {
        return employeeRepository.countActiveEmployees();
    }

    public long countTotalEmployees() {
        return employeeRepository.countTotalEmployees();
    }
}