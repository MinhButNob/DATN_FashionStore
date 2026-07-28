package com.example.backend_fashionstore.auth.repository;

import com.example.backend_fashionstore.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository
        extends JpaRepository<Customer, Integer> {

    boolean existsByUser_UserId(Integer userId);

    Optional<Customer> findByUser_UserId(Integer userId);

    // Sử dụng Spring Data JPA naming convention
    Optional<Customer> findByUser_Email(String email);

    boolean existsByPhone(String phone);


//    java.util.Optional<Customer> findByUser_Email(String email);

    Optional<Customer> findByPhone(String phone);

}