package com.example.backend_fashionstore.enums;

public enum PaymentStatus {
    PENDING,    // Chờ thanh toán
    SUCCESS,    // Thanh toán thành công
    FAILED,     // Thanh toán thất bại
    CANCELLED,  // Khách hủy giao dịch
    UNPAID, REFUNDED    // Đã hoàn tiền
}
