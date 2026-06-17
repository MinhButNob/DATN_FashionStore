package com.example.backend_fashionstore.QuanLyDonHang.controller;

import com.example.backend_fashionstore.QuanLyDonHang.dto.OrderStatisticsDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.AdminOrderResponseDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.UpdateOrderStatusRequest;
import com.example.backend_fashionstore.QuanLyDonHang.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ResponseEntity<Page<AdminOrderResponseDTO>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AdminOrderResponseDTO> orders = adminOrderService.getAllOrders(status, fromDate, toDate, keyword, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderResponseDTO> getOrderDetail(@PathVariable Long orderId) {
        return ResponseEntity.ok(adminOrderService.getOrderDetail(orderId));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<AdminOrderResponseDTO> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request
    ) {
        AdminOrderResponseDTO order = adminOrderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{orderId}/payment-status")
    public ResponseEntity<AdminOrderResponseDTO> updatePaymentStatus(
            @PathVariable Long orderId,
            @RequestParam String paymentStatus
    ) {
        AdminOrderResponseDTO order = adminOrderService.updatePaymentStatus(orderId, paymentStatus);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/statistics")
    public ResponseEntity<OrderStatisticsDTO> getStatistics() {
        return ResponseEntity.ok(adminOrderService.getStatistics());
    }

    @GetMapping("/status/counts")
    public ResponseEntity<Map<String, Long>> getOrderStatusCounts() {
        return ResponseEntity.ok(adminOrderService.getOrderStatusCounts());
    }
}