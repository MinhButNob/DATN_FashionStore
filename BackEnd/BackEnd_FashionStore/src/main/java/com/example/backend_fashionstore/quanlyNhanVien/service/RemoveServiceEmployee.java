package com.example.backend_fashionstore.quanlyNhanVien.service;

import com.example.backend_fashionstore.entity.Employee;
import com.example.backend_fashionstore.enums.UserStatus;
import com.example.backend_fashionstore.quanlyNhanVien.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RemoveServiceEmployee {

    private final EmployeeRepository employeeRepository;
    private final UpdateServiceEmployee updateServiceEmployee;

    // ✅ SỬA: Soft delete (chuyển status thành DELETED) thay vì xóa cứng
    @Transactional
    public void deleteEmployee(Integer id) {
        updateServiceEmployee.softDeleteEmployee(id);
    }

    // ✅ Xóa cứng (nếu cần, nhưng không khuyến khích)
    @Transactional
    public void permanentDeleteEmployee(Integer id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy nhân viên với ID: " + id);
        }
        employeeRepository.deleteById(id);
    }
}