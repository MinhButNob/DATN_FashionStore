package com.example.backend_fashionstore.customer.cart.repository;

import com.example.backend_fashionstore.entity.Cart;
import com.example.backend_fashionstore.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository("customerCartRepository")
public interface CartRepository extends JpaRepository<Cart, Integer> {

    Optional<Cart> findByCustomerAndDeletedAtIsNull(Customer customer);

    @Query("SELECT c FROM Cart c WHERE c.customer.customerId = :customerId AND c.deletedAt IS NULL")
    Optional<Cart> findByCustomerIdAndDeletedAtIsNull(@Param("customerId") Integer customerId);

    Optional<Cart> findByCustomer_CustomerIdAndDeletedAtIsNull(Integer customerId);

    boolean existsByCustomer_CustomerIdAndDeletedAtIsNull(Integer customerId);

    @Modifying
    @Transactional
    @Query("UPDATE Cart c SET c.deletedAt = CURRENT_TIMESTAMP WHERE c.customer.customerId = :customerId AND c.deletedAt IS NULL")
    void softDeleteByCustomerId(@Param("customerId") Integer customerId);

    @Query("SELECT c.customer.customerId FROM Cart c WHERE c.customer.user.email = :email")
    Optional<Integer> findCustomerIdByEmail(@Param("email") String email);

    @Query("SELECT c FROM Cart c WHERE c.customer.user.email = :email AND c.deletedAt IS NULL")
    Optional<Cart> findByCustomerEmailAndDeletedAtIsNull(@Param("email") String email);
}