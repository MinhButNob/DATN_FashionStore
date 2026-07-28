package com.example.backend_fashionstore.customer.wishlist.service;


import com.example.backend_fashionstore.customer.wishlist.dto.response.WishlistProductResponse;

import java.util.List;


import com.example.backend_fashionstore.customer.wishlist.dto.response.WishlistResponseDTO;

public interface WishlistService {

    WishlistResponseDTO getWishlist(Integer customerId);

    WishlistResponseDTO addToWishlist(Integer customerId, Integer productDetailId);

    WishlistResponseDTO removeFromWishlist(Integer customerId, Integer productDetailId);

    WishlistResponseDTO hardRemoveFromWishlist(Integer customerId, Integer productDetailId);

    WishlistResponseDTO toggleWishlist(Integer customerId, Integer productDetailId);

}