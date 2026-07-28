package com.example.backend_fashionstore.QuanLyDonHang.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsDTO {
    // Thống kê đơn hàng cơ bản
    private long totalOrders;
    private long completedOrders;
    private long pendingOrders;
    private long cancelledOrders;
    private long processingOrders;
    private long shippingOrders;
    private long confirmedOrders;
    private long deliveredOrders;

    // Thống kê doanh thu
    private BigDecimal todayRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal totalRevenue;           // Tổng doanh thu từ đơn hàng COMPLETED
    private BigDecimal onlineRevenue;          // Doanh thu bán hàng online (COMPLETED)
    private BigDecimal returnRefundAmount;     // Tổng tiền hoàn trả
    private BigDecimal netRevenue;             // Doanh thu thực tế = OnlineRevenue - ReturnRefundAmount

    // Thống kê hoàn hàng chi tiết
    private long returnRequested;              // Yêu cầu hoàn hàng
    private long returnApproved;               // Đã duyệt hoàn hàng
    private long returnReceived;               // Đã nhận hàng trả
    private long returnPending;                // Đang thu hồi hàng
    private long returnCompleted;              // Đã hoàn hàng (RETURNED)
    private long returnRejected;               // Từ chối hoàn trả (REJECTED)
    private long returnedOrders;               // Tổng đơn hoàn hàng

    // Thống kê đã hủy chi tiết
    private long cancelledByCustomer;          // Khách hủy
    private long cancelledByAdmin;             // Admin hủy
    private long cancelledBySystem;            // Hệ thống hủy
}