package com.example.backend_fashionstore.quanlyNhanVien.service;


import com.example.backend_fashionstore.entity.Employee;
import com.example.backend_fashionstore.exception.BusinessException;
import com.example.backend_fashionstore.quanlyNhanVien.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    /**
     * Lấy employeeId từ userId
     */
    public Integer getEmployeeIdByUserId(Integer userId) {
        return employeeRepository.findByEmployeeUserId(userId)
                .map(Employee::getEmployeeId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin nhân viên cho user_id: " + userId));
    }

    /**
     * Lấy Employee từ userId
     */
    public Employee getEmployeeByUserId(Integer userId) {
        return employeeRepository.findByEmployeeUserId(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin nhân viên cho user_id: " + userId));
    }

    /**
     * Kiểm tra user có phải nhân viên không
     */
    public boolean isEmployee(Integer userId) {
        return employeeRepository.existsByEmployeeUserId(userId);
    }
}