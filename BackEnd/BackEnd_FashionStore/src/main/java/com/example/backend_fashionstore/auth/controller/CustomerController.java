package com.example.backend_fashionstore.auth.controller;

import com.example.backend_fashionstore.auth.dto.*;
import com.example.backend_fashionstore.auth.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class CustomerController {

    private final CustomerService customerService;

    //Lấy danh sách khách hàng (phân trang + tìm kiếm)
    @GetMapping
    public ResponseEntity<Page<CustomerDTO>> getAllCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "customerId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<CustomerDTO> customers = customerService.getAllCustomers(keyword, pageable);
        return ResponseEntity.ok(customers);
    }

    //Lấy chi tiết khách hàng theo ID
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Integer id) {
        CustomerDTO customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customer);
    }

    //Thêm khách hàng mới
    @PostMapping
    public ResponseEntity<CustomerDTO> createCustomer(@Valid @RequestBody CustomerCreateDTO createDTO) {
        CustomerDTO newCustomer = customerService.createCustomer(createDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(newCustomer);
    }

    //Cập nhật thông tin khách hàng
    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> updateCustomer(
            @PathVariable Integer id,
            @Valid @RequestBody CustomerUpdateDTO updateDTO
    ) {
        CustomerDTO updatedCustomer = customerService.updateCustomer(id, updateDTO);
        return ResponseEntity.ok(updatedCustomer);
    }

    //Xóa khách hàng (soft delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Integer id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    // Khóa/Mở khóa tài khoản khách hàng
    @PatchMapping("/{id}/toggle-lock")
    public ResponseEntity<Void> toggleLockCustomer(@PathVariable Integer id) {
        customerService.toggleLockCustomer(id);
        return ResponseEntity.ok().build();
    }

    //Thống kê số lượng khách hàng
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsDTO> getStatistics() {
        StatisticsDTO statistics = customerService.getStatistics();
        return ResponseEntity.ok(statistics);
    }
}