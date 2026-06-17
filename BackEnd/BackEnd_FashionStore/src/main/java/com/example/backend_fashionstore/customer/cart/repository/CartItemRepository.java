package com.example.backend_fashionstore.customer.cart.repository;

import com.example.backend_fashionstore.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    List<CartItem> findByCart_CartIdAndDeletedAtIsNull(Integer cartId);

    Optional<CartItem> findByCart_CartIdAndProductDetail_ProductDetailIdAndDeletedAtIsNull(
            Integer cartId, Integer productDetailId);

    @Modifying
    @Transactional
    @Query("UPDATE CartItem ci SET ci.deletedAt = CURRENT_TIMESTAMP WHERE ci.cart.cartId = :cartId AND ci.deletedAt IS NULL")
    void softDeleteByCartId(@Param("cartId") Integer cartId);

    @Modifying
    @Transactional
    @Query("UPDATE CartItem ci SET ci.quantity = :quantity WHERE ci.cartItemId = :cartItemId")
    void updateQuantity(@Param("cartItemId") Integer cartItemId, @Param("quantity") Integer quantity);

    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItem ci WHERE ci.cart.cartId = :cartId AND ci.deletedAt IS NULL")
    Integer countTotalItemsByCartId(@Param("cartId") Integer cartId);
}