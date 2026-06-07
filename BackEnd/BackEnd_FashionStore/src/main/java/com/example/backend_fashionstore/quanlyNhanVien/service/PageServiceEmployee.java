package com.example.backend_fashionstore.quanlyNhanVien.service;

import com.example.backend_fashionstore.entity.Employee;
import com.example.backend_fashionstore.enums.UserStatus;
import com.example.backend_fashionstore.quanlyNhanVien.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PageServiceEmployee {

    private final EmployeeRepository employeeRepository;

    public Page<Employee> searchEmployees(String keyword, String status, int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // ✅ Xử lý filter theo user.status
        if (status != null && !status.isBlank()) {
            // Nếu filter theo DELETED, ACTIVE, BANNED - lọc theo user.status
            try {
                UserStatus userStatus = UserStatus.valueOf(status);
                return employeeRepository.findByUserStatus(userStatus, pageable);
            } catch (IllegalArgumentException e) {
                // Nếu không phải user status, lọc theo employeeStatus
                String st = status.isBlank() ? null : status;
                String kw = (keyword != null && keyword.isBlank()) ? null : keyword;
                return employeeRepository.searchEmployees(kw, st, pageable);
            }
        } else {
            // Không filter, lấy tất cả (bao gồm cả đã xóa) để hiển thị chế độ chỉ xem
            return employeeRepository.findAllWithDeleted(pageable);
        }
    }
}