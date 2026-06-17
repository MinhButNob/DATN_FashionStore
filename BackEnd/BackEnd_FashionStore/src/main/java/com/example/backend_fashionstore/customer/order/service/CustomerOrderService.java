package com.example.backend_fashionstore.customer.order.service;

import com.example.backend_fashionstore.customer.order.dto.request.OrderRequestDTO;
import com.example.backend_fashionstore.customer.order.dto.response.OrderResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerOrderService {
    OrderResponseDTO createOrder(Integer customerId, OrderRequestDTO request);
    OrderResponseDTO getOrder(Long orderId, Integer customerId);
    Page<OrderResponseDTO> getMyOrders(Integer customerId, String status, Pageable pageable);
    OrderResponseDTO cancelOrder(Long orderId, Integer customerId, String reason);
}