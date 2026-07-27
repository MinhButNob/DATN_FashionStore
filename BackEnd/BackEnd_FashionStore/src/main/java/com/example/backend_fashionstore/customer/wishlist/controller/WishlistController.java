package com.example.backend_fashionstore.customer.wishlist.controller;

import com.example.backend_fashionstore.QuanLySanPham.dto.response.ApiResponse;
import com.example.backend_fashionstore.auth.repository.CustomerRepository;
import com.example.backend_fashionstore.customer.wishlist.dto.request.WishlistRequestDTO;
import com.example.backend_fashionstore.customer.wishlist.dto.response.WishlistResponseDTO;
import com.example.backend_fashionstore.entity.Customer;
import com.example.backend_fashionstore.customer.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final CustomerRepository customerRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<WishlistResponseDTO>> getWishlist(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        WishlistResponseDTO wishlist = wishlistService.getWishlist(customerId);
        return ResponseEntity.ok(ApiResponse.success(wishlist));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<WishlistResponseDTO>> addToWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody WishlistRequestDTO request
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        WishlistResponseDTO wishlist = wishlistService.addToWishlist(customerId, request.getProductDetailId());
        return ResponseEntity.ok(ApiResponse.success("Đã thêm vào danh sách yêu thích", wishlist));
    }

    @DeleteMapping("/items/{productDetailId}")
    public ResponseEntity<ApiResponse<WishlistResponseDTO>> removeFromWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Integer productDetailId
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        WishlistResponseDTO wishlist = wishlistService.removeFromWishlist(customerId, productDetailId);
        return ResponseEntity.ok(ApiResponse.success("Đã xoá khỏi danh sách yêu thích", wishlist));
    }

    private Integer getCustomerIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }
        String email = userDetails.getUsername();
        Customer customer = customerRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với email: " + email));
        return customer.getCustomerId();
    }
}