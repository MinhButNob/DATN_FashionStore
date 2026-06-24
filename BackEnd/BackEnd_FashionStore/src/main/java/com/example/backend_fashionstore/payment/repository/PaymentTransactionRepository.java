package com.example.backend_fashionstore.payment.repository;

import com.example.backend_fashionstore.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, Long> {
}