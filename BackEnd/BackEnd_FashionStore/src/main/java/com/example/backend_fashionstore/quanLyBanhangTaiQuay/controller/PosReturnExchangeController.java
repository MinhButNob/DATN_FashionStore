package com.example.backend_fashionstore.quanLyBanhangTaiQuay.controller;

import com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto.*;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.service.PosReturnExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pos/return-exchange")
@RequiredArgsConstructor
public class PosReturnExchangeController {

    private final PosReturnExchangeService returnExchangeService;

    /**
     * Tìm đơn hàng theo số điện thoại
     */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/orders/search")
    public ResponseEntity<List<PosOrderResponse>> searchOrdersByPhone(
            @RequestParam String phone) {
        return ResponseEntity.ok(returnExchangeService.findOrdersByPhone(phone));
    }

    /**
     * Tạo phiếu đổi/trả hàng
     */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/create")
    public ResponseEntity<PosReturnExchangeResponse> createReturnExchange(
            @RequestBody PosReturnExchangeRequest request) {
        return ResponseEntity.ok(returnExchangeService.createReturnExchange(request));
    }

    /**
     * Lấy danh sách tất cả phiếu đổi/trả
     */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/list")
    public ResponseEntity<List<PosReturnExchangeResponse>> getAllReturnExchanges() {
        return ResponseEntity.ok(returnExchangeService.getAllReturnExchanges());
    }

    /**
     * Lấy chi tiết phiếu đổi/trả theo ID
     */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<PosReturnExchangeResponse> getReturnExchangeDetail(
            @PathVariable Integer id) {
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
        return ResponseEntity.ok(returnExchangeService.updateReturnExchangeStatus(id, request.getStatus()));
    }

    /**
     * Hủy phiếu đổi/trả
     */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReturnExchange(
            @PathVariable Integer id) {
        returnExchangeService.cancelReturnExchange(id);
        return ResponseEntity.noContent().build();
    }
}