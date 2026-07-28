package com.example.backend_fashionstore.customer.wishlist.service;

import com.example.backend_fashionstore.customer.wishlist.dto.response.WishlistProductResponse;

import java.util.List;

public interface WishlistService {
    List<WishlistProductResponse> getWishlist(Integer customerId);
    void toggleWishlist(Integer customerId, Integer productId);
}