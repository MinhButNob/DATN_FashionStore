package com.example.backend_fashionstore.customer.order.repository;

import com.example.backend_fashionstore.entity.OrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {
    List<OrderHistory> findByOrder_OrderIdOrderByTimestampDesc(Long orderId);
}