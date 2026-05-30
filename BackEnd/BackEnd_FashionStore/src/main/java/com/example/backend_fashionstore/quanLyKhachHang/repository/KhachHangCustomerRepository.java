package com.example.backend_fashionstore.quanLyKhachHang.repository;

import com.example.backend_fashionstore.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhachHangCustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByUserUserId(Integer userId);

    Optional<Customer> findByUserEmail(String email);

    @Query("SELECT c FROM Customer c WHERE " +
            "c.user.isActive = true AND (" +
            "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "c.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<Customer> searchCustomers(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.customerStatus = 'LOCKED' AND c.user.isActive = true")
    long countLockedCustomers();

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.customerStatus = 'ACTIVE' AND c.user.isActive = true")
    long countActiveCustomers();

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.user.isActive = true")
    long countAllActiveCustomers();
}