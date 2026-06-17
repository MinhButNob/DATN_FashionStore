package com.example.backend_fashionstore.QuanLyDonHang.service;


import com.example.backend_fashionstore.QuanLyDonHang.dto.OrderStatisticsDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.AdminOrderResponseDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.UpdateOrderStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Map;

public interface AdminOrderService {
    Page<AdminOrderResponseDTO> getAllOrders(String status, LocalDateTime fromDate, LocalDateTime toDate, String keyword, Pageable pageable);
    AdminOrderResponseDTO getOrderDetail(Long orderId);
    AdminOrderResponseDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);
    AdminOrderResponseDTO updatePaymentStatus(Long orderId, String paymentStatus);
    OrderStatisticsDTO getStatistics();
    Map<String, Long> getOrderStatusCounts();
}