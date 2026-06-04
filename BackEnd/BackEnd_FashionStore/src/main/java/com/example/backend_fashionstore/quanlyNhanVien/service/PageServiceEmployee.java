package com.example.backend_fashionstore.quanlyNhanVien.service;

import com.example.backend_fashionstore.entity.Employee;
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
        String st = (status != null && status.isBlank()) ? null : status;

        return employeeRepository.searchEmployees(kw, st, pageable);
    }
}