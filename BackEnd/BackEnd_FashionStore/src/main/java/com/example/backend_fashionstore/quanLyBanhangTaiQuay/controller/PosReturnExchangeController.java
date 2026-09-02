package com.example.backend_fashionstore.quanLyBanhangTaiQuay.controller;

import com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto.*;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.service.PosReturnExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pos/return-exchange")
@RequiredArgsConstructor
public class PosReturnExchangeController {

    private final PosReturnExchangeService returnExchangeService;

    /**
     * Tìm đơn hàng theo mã hóa đơn để hoàn trả
     */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/orders/search")
    public ResponseEntity<?> searchOrderByInvoiceNumber(
            @RequestParam String invoiceNumber) {
        log.info("🔍 [API] Tìm đơn hàng theo mã hóa đơn: '{}'", invoiceNumber);
        try {
            PosOrderResponse response = returnExchangeService.findOrderByInvoiceNumber(invoiceNumber);
            log.info("✅ [API] Tìm thấy đơn hàng: {}", response.getPosOrderInvoiceNumber());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [API] Lỗi tìm đơn hàng: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Tạo phiếu đổi/trả hàng
     */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/create")
    public ResponseEntity<?> createReturnExchange(
            @RequestBody PosReturnExchangeRequest request) {
        log.info("📋 [API] Tạo phiếu đổi/trả cho đơn hàng: {}", request.getOrderId());
        try {
            PosReturnExchangeResponse response = returnExchangeService.createReturnExchange(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [API] Lỗi tạo phiếu: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Lấy danh sách tất cả phiếu đổi/trả
     */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/list")
    public ResponseEntity<List<PosReturnExchangeResponse>> getAllReturnExchanges() {
        log.info("📋 [API] Lấy danh sách tất cả phiếu đổi/trả");
        return ResponseEntity.ok(returnExchangeService.getAllReturnExchanges());
    }

    /**
     * Lấy chi tiết phiếu đổi/trả theo ID
     */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<PosReturnExchangeResponse> getReturnExchangeDetail(
            @PathVariable Integer id) {
        log.info("📋 [API] Lấy chi tiết phiếu đổi/trả ID: {}", id);
        return ResponseEntity.ok(returnExchangeService.getReturnExchangeDetail(id));
    }

    /**
     * Cập nhật trạng thái phiếu đổi/trả
     */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PutMapping("/{id}/status")
    public ResponseEntity<PosReturnExchangeResponse> updateReturnExchangeStatus(
            @PathVariable Integer id,
            @RequestBody PosReturnExchangeStatusRequest request) {
        log.info("📋 [API] Cập nhật trạng thái phiếu ID: {}, Status: {}", id, request.getStatus());
        return ResponseEntity.ok(returnExchangeService.updateReturnExchangeStatus(id, request.getStatus()));
    }

    /**
     * Hủy phiếu đổi/trả
     */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReturnExchange(
            @PathVariable Integer id) {
        log.info("📋 [API] Hủy phiếu đổi/trả ID: {}", id);
        returnExchangeService.cancelReturnExchange(id);
        return ResponseEntity.noContent().build();
    }
}