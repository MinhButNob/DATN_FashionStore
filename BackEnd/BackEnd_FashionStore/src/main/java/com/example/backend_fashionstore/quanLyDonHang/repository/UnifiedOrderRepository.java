package com.example.backend_fashionstore.quanLyDonHang.repository;

import com.example.backend_fashionstore.entity.Order;
import com.example.backend_fashionstore.entity.PosOrder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UnifiedOrderRepository {

    // Lấy tất cả đơn online
    List<Order> findAllOnlineOrders(LocalDateTime startDate, LocalDateTime endDate, String status);

    // Lấy tất cả đơn POS
    List<PosOrder> findAllPosOrders(LocalDateTime startDate, LocalDateTime endDate, String status);

    // Đếm số lượng đơn online
    long countOnlineOrders(LocalDateTime startDate, LocalDateTime endDate, String status);

    // Đếm số lượng đơn POS
    long countPosOrders(LocalDateTime startDate, LocalDateTime endDate, String status);

    // Tổng doanh thu online
    BigDecimal sumOnlineRevenue(LocalDateTime startDate, LocalDateTime endDate);

    // Tổng doanh thu POS
    BigDecimal sumPosRevenue(LocalDateTime startDate, LocalDateTime endDate);
}
