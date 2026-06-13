package com.example.backend_fashionstore.quanLyVoucher.controller;

import com.example.backend_fashionstore.quanLyVoucher.dto.*;
import com.example.backend_fashionstore.quanLyVoucher.service.VoucherService;
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
@RequestMapping("/api/admin/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class VoucherController {

    private final VoucherService voucherService;

    // Lấy danh sách voucher (phân trang + lọc + tìm kiếm)
    @GetMapping
    public ResponseEntity<Page<VoucherDTO>> getAllVouchers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "voucherId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<VoucherDTO> vouchers = voucherService.filterAndSearchVouchers(status, keyword, pageable);
        return ResponseEntity.ok(vouchers);
    }

    // Lấy danh sách đã xóa
    @GetMapping("/deleted")
    public ResponseEntity<Page<VoucherDTO>> getDeletedVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(voucherService.getDeletedVouchers(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VoucherDTO> getVoucherById(@PathVariable Integer id) {
        return ResponseEntity.ok(voucherService.getVoucherById(id));
    }

    @PostMapping
    public ResponseEntity<VoucherDTO> createVoucher(@Valid @RequestBody VoucherCreateDTO createDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(voucherService.createVoucher(createDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VoucherDTO> updateVoucher(
            @PathVariable Integer id,
            @Valid @RequestBody VoucherUpdateDTO updateDTO
    ) {
        return ResponseEntity.ok(voucherService.updateVoucher(id, updateDTO));
    }

    // Xóa mềm (ẩn)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteVoucher(@PathVariable Integer id) {
        voucherService.softDeleteVoucher(id);
        return ResponseEntity.noContent().build();
    }

    // Khôi phục
    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restoreVoucher(@PathVariable Integer id) {
        voucherService.restoreVoucher(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Void> toggleVoucherStatus(@PathVariable Integer id) {
        voucherService.toggleVoucherStatus(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/statistics")
    public ResponseEntity<VoucherStatisticsDTO> getStatistics() {
        return ResponseEntity.ok(voucherService.getStatistics());
    }
}