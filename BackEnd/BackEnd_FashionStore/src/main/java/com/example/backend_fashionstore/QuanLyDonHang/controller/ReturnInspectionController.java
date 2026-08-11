package com.example.backend_fashionstore.QuanLyDonHang.controller;

import com.example.backend_fashionstore.QuanLyDonHang.dto.request.ReturnInspectionRequestDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.ReturnInspectionResponseDTO;
import com.example.backend_fashionstore.QuanLyDonHang.service.ReturnInspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/returns")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class ReturnInspectionController {
    private final ReturnInspectionService returnInspectionService;

    @PostMapping("/{returnRequestId}/inspect")
    public ResponseEntity<ReturnInspectionResponseDTO> inspect(
            @PathVariable Long returnRequestId,
            @Valid @RequestBody ReturnInspectionRequestDTO request) {
        return ResponseEntity.ok(returnInspectionService.inspect(returnRequestId, request));
    }
}
