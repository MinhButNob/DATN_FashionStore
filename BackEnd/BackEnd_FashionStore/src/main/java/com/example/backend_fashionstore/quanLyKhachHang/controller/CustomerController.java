package com.example.backend_fashionstore.quanLyKhachHang.controller;

import com.example.backend_fashionstore.quanLyKhachHang.dto.*;
import com.example.backend_fashionstore.quanLyKhachHang.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class CustomerController {

    private final CustomerService customerService;

    // ✅ Danh sách khách hàng
    @GetMapping
    public ResponseEntity<Page<CustomerDTO>> getAllCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "customerId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(customerService.getAllCustomers(keyword, status, pageable));
    }

    // ✅ Chi tiết khách hàng
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Integer id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    // ❌ XÓA: createCustomer - khách tự đăng ký, admin không thêm khách

    // ❌ XÓA: updateCustomer - khách tự cập nhật, admin không sửa

    // ✅ Xóa mềm khách hàng
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Integer id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ Khôi phục khách hàng
    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restoreCustomer(@PathVariable Integer id) {
        customerService.restoreCustomer(id);
        return ResponseEntity.ok().build();
    }

    // ✅ Khóa / Mở khóa tài khoản
    @PatchMapping("/{id}/toggle-lock")
    public ResponseEntity<Void> toggleLockCustomer(@PathVariable Integer id) {
        customerService.toggleLockCustomer(id);
        return ResponseEntity.ok().build();
    }

    // ✅ Thống kê
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsDTO> getStatistics() {
        return ResponseEntity.ok(customerService.getStatistics());
    }
}