package com.example.backend_fashionstore.enums;

public enum OrderStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    PROCESSING,
    SHIPPING,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    RETURN_REQUESTED,    // Khách hàng yêu cầu hoàn hàng
    RETURN_APPROVED,      // Admin duyệt hoàn hàng
    RETURN_RECEIVED,      // 🆕 Admin xác nhận đã nhận hàng trả
    RETURN_PENDING,       // Đang thu hồi hàng
    RETURNED,              // Hoàn thành hoàn hàng
    PARTIALLY_RETURNED,
    REJECTED
}
