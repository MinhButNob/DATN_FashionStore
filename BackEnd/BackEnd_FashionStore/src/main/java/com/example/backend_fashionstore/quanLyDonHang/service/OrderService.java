package com.example.backend_fashionstore.quanLyDonHang.service;

import com.example.backend_fashionstore.quanLyDonHang.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    List<OrderResponse> getAllOrders();
    OrderResponse updateOrderStatus(Long orderId, String newStatus);
}
