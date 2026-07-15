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

        String kw = (keyword != null && keyword.isBlank()) ? null : keyword;

        // ✅ Xử lý filter theo user.status
        if (status != null && !status.isBlank()) {
            // Nếu filter theo DELETED, ACTIVE, BANNED - lọc theo user.status VÀ áp dụng keyword
            try {
                UserStatus userStatus = UserStatus.valueOf(status);
                return employeeRepository.searchEmployeesByUserStatus(kw, userStatus, pageable);
            } catch (IllegalArgumentException e) {
                // Nếu không phải user status, lọc theo employeeStatus (vẫn áp dụng keyword)
                return employeeRepository.searchEmployees(kw, status, pageable);
            }
        } else {
            // Không filter trạng thái (bao gồm cả đã xóa) - vẫn áp dụng keyword nếu có
            return employeeRepository.searchEmployeesNoStatus(kw, pageable);
        }
    }
}