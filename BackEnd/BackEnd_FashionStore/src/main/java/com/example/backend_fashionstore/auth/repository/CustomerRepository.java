package com.example.backend_fashionstore.auth.repository;

import com.example.backend_fashionstore.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository
        extends JpaRepository<Customer, Integer> {

    boolean existsByUser_UserId(Integer userId);

    java.util.Optional<Customer> findByUser_UserId(Integer userId);

    boolean existsByPhone(String phone);

    java.util.Optional<Customer> findByPhone(String phone);

    java.util.Optional<Customer> findByUser_Email(String email);
}