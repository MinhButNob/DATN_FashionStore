package com.example.backend_fashionstore.customer.order.repository;

import com.example.backend_fashionstore.entity.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {

    List<ShippingAddress> findByCustomer_CustomerId(Integer customerId);

    Optional<ShippingAddress> findByCustomer_CustomerIdAndIsDefaultTrue(Integer customerId);

    @Query("SELECT sa FROM ShippingAddress sa WHERE sa.customer.customerId = :customerId")
    List<ShippingAddress> findAllByCustomerId(@Param("customerId") Integer customerId);

    List<ShippingAddress> findByCustomer_CustomerIdAndStatus(Integer customerId, String status);

    Optional<ShippingAddress> findByCustomer_CustomerIdAndIsDefaultTrueAndStatus(Integer customerId, String status);



}