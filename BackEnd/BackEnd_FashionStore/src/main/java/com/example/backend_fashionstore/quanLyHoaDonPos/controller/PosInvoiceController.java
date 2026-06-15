package com.example.backend_fashionstore.quanLyHoaDonPos.controller;

import com.example.backend_fashionstore.quanLyHoaDonPos.dto.*;
import com.example.backend_fashionstore.quanLyHoaDonPos.service.PosInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.backend_fashionstore.QuanLySanPham.dto.response.ApiResponse;

import java.time.LocalDate;
@RestController
@RequestMapping("/api/pos/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class PosInvoiceController {

    private final PosInvoiceService posInvoiceService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PosInvoiceDTO>>> getInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PosInvoiceFilterDTO filter = new PosInvoiceFilterDTO();
        filter.setPage(page);
        filter.setSize(size);

        return ResponseEntity.ok(ApiResponse.ok(posInvoiceService.searchInvoices(filter)));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<PosInvoiceDTO>>> searchInvoices(@RequestBody PosInvoiceFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.ok(posInvoiceService.searchInvoices(filter)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<PosInvoiceDTO>> getInvoiceDetail(@PathVariable Integer orderId) {
        return ResponseEntity.ok(ApiResponse.ok(posInvoiceService.getInvoiceDetail(orderId)));
    }

    @GetMapping("/number/{invoiceNumber}")
    public ResponseEntity<ApiResponse<PosInvoiceDTO>> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(ApiResponse.ok(posInvoiceService.getInvoiceByNumber(invoiceNumber)));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelInvoice(@PathVariable Integer orderId, @RequestParam String reason) {
        posInvoiceService.cancelInvoice(orderId, reason);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<PosInvoiceStatsDTO>> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.ok(posInvoiceService.getStatistics(startDate, endDate)));
    }

    @GetMapping("/{orderId}/print")
    public ResponseEntity<ApiResponse<PosInvoiceDTO>> printInvoice(@PathVariable Integer orderId) {
        return ResponseEntity.ok(ApiResponse.ok(posInvoiceService.getInvoiceForPrinting(orderId)));
    }
}