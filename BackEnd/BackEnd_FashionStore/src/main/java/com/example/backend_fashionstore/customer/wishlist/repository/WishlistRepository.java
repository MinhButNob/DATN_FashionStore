package com.example.backend_fashionstore.customer.wishlist.repository;

import com.example.backend_fashionstore.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {

    @Query("SELECT w FROM Wishlist w WHERE w.customer.customerId = :customerId AND w.deletedAt IS NULL")
    Optional<Wishlist> findByCustomerIdAndDeletedAtIsNull(@Param("customerId") Integer customerId);
}