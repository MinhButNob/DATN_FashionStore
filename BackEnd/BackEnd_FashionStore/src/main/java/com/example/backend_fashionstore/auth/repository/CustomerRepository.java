package com.example.backend_fashionstore.auth.repository;


import com.example.backend_fashionstore.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface CustomerRepository
        extends JpaRepository<Customer, Integer> {

    boolean existsByUser_UserId(Integer userId);

    java.util.Optional<Customer> findByUser_UserId(Integer userId);

    @Query("SELECT c FROM Customer c JOIN c.user u WHERE u.email = :email")
    Optional<Customer> findByUser_Email(@Param("email") String email);

    boolean existsByPhone(String phone);

    java.util.Optional<Customer> findByPhone(String phone);

//    java.util.Optional<Customer> findByUser_Email(String email);
}