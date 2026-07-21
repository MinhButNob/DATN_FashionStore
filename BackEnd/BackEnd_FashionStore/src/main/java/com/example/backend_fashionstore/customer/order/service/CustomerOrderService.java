package com.example.backend_fashionstore.customer.order.service;

import com.example.backend_fashionstore.customer.order.dto.request.OrderRequestDTO;
import com.example.backend_fashionstore.customer.order.dto.request.ReturnRequestDTO;
import com.example.backend_fashionstore.customer.order.dto.response.OrderResponseDTO;
import com.example.backend_fashionstore.customer.order.dto.response.ReturnRequestResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerOrderService {

    // Tạo đơn hàng
    OrderResponseDTO createOrder(Integer customerId, OrderRequestDTO request);

    // Lấy chi tiết đơn hàng
    OrderResponseDTO getOrder(Long orderId, Integer customerId);

    // Lấy danh sách đơn hàng của khách hàng
    Page<OrderResponseDTO> getMyOrders(Integer customerId, String status, Pageable pageable);

    // Hủy đơn hàng
    OrderResponseDTO cancelOrder(Long orderId, Integer customerId, String reason);

    // ===== HOÀN HÀNG =====

    // Yêu cầu hoàn hàng (có ảnh và nội dung, hỗ trợ hoàn theo sản phẩm)
    ReturnRequestResponseDTO requestReturn(Long orderId, Integer customerId, ReturnRequestDTO request);

    // Lấy danh sách yêu cầu hoàn hàng của khách hàng
    Page<ReturnRequestResponseDTO> getMyReturnRequests(Integer customerId, Pageable pageable);

    // 🆕 Lấy chi tiết yêu cầu hoàn hàng
    ReturnRequestResponseDTO getReturnRequestDetail(Long orderId, Integer customerId);

    // 🆕 Trừ kho khi chuyển từ PROCESSING sang SHIPPING
    void deductStockForOrder(Long orderId);

    // 🆕 Hoàn kho khi hủy đơn hoặc hoàn hàng
    void restoreStockForOrder(Long orderId);
}