package com.example.backend_fashionstore.customer.wishlist.repository;

import com.example.backend_fashionstore.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Integer> {

    @Query("SELECT wi FROM WishlistItem wi WHERE wi.wishlist.wishlistId = :wishlistId " +
            "AND wi.wishlistItemDeletedAt IS NULL ORDER BY wi.wishlistItemAddedAt DESC")
    List<WishlistItem> findActiveByWishlistId(@Param("wishlistId") Integer wishlistId);

    @Query("SELECT wi FROM WishlistItem wi WHERE wi.wishlist.wishlistId = :wishlistId " +
            "AND wi.productDetail.productDetailId = :productDetailId AND wi.wishlistItemDeletedAt IS NULL")
    Optional<WishlistItem> findActiveItem(@Param("wishlistId") Integer wishlistId,
                                          @Param("productDetailId") Integer productDetailId);

    @Query("SELECT wi FROM WishlistItem wi WHERE wi.wishlist.wishlistId = :wishlistId " +
            "AND wi.productDetail.productDetailId = :productDetailId")
    Optional<WishlistItem> findByWishlistIdAndProductDetailId(@Param("wishlistId") Integer wishlistId,
                                                              @Param("productDetailId") Integer productDetailId);
}