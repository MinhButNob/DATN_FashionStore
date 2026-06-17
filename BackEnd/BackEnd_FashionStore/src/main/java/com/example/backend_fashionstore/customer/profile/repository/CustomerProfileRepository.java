package com.example.backend_fashionstore.customer.profile.repository;

import com.example.backend_fashionstore.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("customerProfileRepository")  // ← TÊN BEAN RIÊNG, KHÔNG TRÙNG
public interface CustomerProfileRepository extends JpaRepository<Customer, Integer> {

    // Tìm customer theo email
    @Query("SELECT c FROM Customer c WHERE c.user.email = :email")
    Optional<Customer> findByUserEmail(@Param("email") String email);

    // Tìm customer theo user_id
    @Query("SELECT c FROM Customer c WHERE c.user.userId = :userId")
    Optional<Customer> findByUserId(@Param("userId") Integer userId);

    // Kiểm tra tồn tại theo email
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.user.email = :email")
    boolean existsByUserEmail(@Param("email") String email);

    // Tìm customer kèm thông tin user
    @Query("SELECT c FROM Customer c JOIN FETCH c.user WHERE c.user.email = :email")
    Optional<Customer> findCustomerWithUserByEmail(@Param("email") String email);
}