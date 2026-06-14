package com.example.backend_fashionstore.quanLyDonHang.repository;

import com.example.backend_fashionstore.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrder_OrderIdOrderByPaymentDateDesc(Long orderId);
}
