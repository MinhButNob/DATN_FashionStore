package com.example.backend_fashionstore.auth.repository;

import com.example.backend_fashionstore.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository
        extends JpaRepository<Cart, Integer> {

    boolean existsByCustomer_CustomerId(Integer customerId);
}

