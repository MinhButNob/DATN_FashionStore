package com.example.backend_fashionstore.quanLyDonHang.controller;

import com.example.backend_fashionstore.quanLyDonHang.dto.OrderResponse;
import com.example.backend_fashionstore.quanLyDonHang.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin("*")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy danh sách đơn hàng thành công");
        response.put("data", orders);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newStatus = payload.get("status");
        OrderResponse updatedOrder = orderService.updateOrderStatus(id, newStatus);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cập nhật trạng thái thành công");
        response.put("data", updatedOrder);
        return ResponseEntity.ok(response);
    }
}
