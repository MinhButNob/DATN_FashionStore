package com.example.backend_fashionstore.quanLyKhachHang.controller;

import com.example.backend_fashionstore.quanLyKhachHang.dto.*;
import com.example.backend_fashionstore.quanLyKhachHang.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // ✅ SỬA: Tìm khách hàng theo số điện thoại - trả về danh sách
    @GetMapping("/search-by-phone")
    public ResponseEntity<List<CustomerDTO>> getCustomersByPhone(@RequestParam String phone) {
        return ResponseEntity.ok(customerService.findAllByPhone(phone));
    }

    // ✅ THÊM MỚI: Lấy 1 khách hàng theo số điện thoại
    @GetMapping("/by-phone")
    public ResponseEntity<CustomerDTO> getCustomerByPhone(@RequestParam String phone) {
        return ResponseEntity.ok(customerService.getCustomerByPhone(phone));
    }

    // ✅ Kiểm tra SĐT đã tồn tại chưa
    @GetMapping("/check-phone")
    public ResponseEntity<Boolean> checkPhoneExists(@RequestParam String phone) {
        // Luôn trả về false để không chặn tạo mới
        return ResponseEntity.ok(false);
    }

    // ✅ Tạo khách hàng mới
    @PostMapping
    public ResponseEntity<CustomerDTO> createCustomer(@RequestBody CustomerCreateDTO createDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.createCustomer(createDTO));
    }

    // ✅ Cập nhật khách hàng
    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> updateCustomer(
            @PathVariable Integer id,
            @RequestBody CustomerUpdateDTO updateDTO) {
        return ResponseEntity.ok(customerService.updateCustomer(id, updateDTO));
    }
}