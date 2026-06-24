package com.example.backend_fashionstore.payment.repository;

import com.example.backend_fashionstore.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTxnRef(String txnRef);

    Optional<Payment> findByOrder_OrderId(Long orderId);
}