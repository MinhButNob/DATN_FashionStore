package com.example.backend_fashionstore.auth.repository;

import com.example.backend_fashionstore.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;  // ✅ ĐÚNG
import java.util.List;
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

    @Query("SELECT c FROM Customer c WHERE c.user.status <> 'DELETED' " +
            "AND (LOWER(c.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR c.phone LIKE CONCAT('%', :keyword, '%'))")
    List<Customer> searchCustomers(@Param("keyword") String keyword);

    // ✅ Thêm method này
    @Query("SELECT c FROM Customer c WHERE c.user.status <> 'DELETED'")
    List<Customer> findAllActiveCustomers();

    @Query("SELECT c FROM Customer c WHERE c.user.status <> 'DELETED'")
    Page<Customer> findAllActiveCustomers(Pageable pageable);
}