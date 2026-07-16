package com.example.backend_fashionstore.customer.wishlist.repository;

import com.example.backend_fashionstore.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Integer> {
    List<WishlistItem> findByWishlist_WishlistIdAndWishlistItemStatus(Integer wishlistId, String status);

    Optional<WishlistItem> findByWishlist_WishlistIdAndProductDetail_Product_ProductIdAndWishlistItemStatus(
            Integer wishlistId, Integer productId, String status);
}
