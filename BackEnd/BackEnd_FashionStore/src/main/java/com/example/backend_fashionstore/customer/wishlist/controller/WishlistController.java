package com.example.backend_fashionstore.customer.wishlist.controller;

import com.example.backend_fashionstore.QuanLySanPham.dto.response.ApiResponse;
import com.example.backend_fashionstore.customer.wishlist.dto.response.WishlistProductResponse;
import com.example.backend_fashionstore.customer.wishlist.service.WishlistService;
import com.example.backend_fashionstore.entity.Customer;
import com.example.backend_fashionstore.quanLyKhachHang.repository.KhachHangCustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/customer/wishlist")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WishlistController {

    private final WishlistService wishlistService;
    private final KhachHangCustomerRepository customerRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistProductResponse>>> getWishlist(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        List<WishlistProductResponse> wishlist = wishlistService.getWishlist(customerId);
        return ResponseEntity.ok(ApiResponse.success(wishlist));
    }

    @PostMapping("/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Integer productId
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        wishlistService.toggleWishlist(customerId, productId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    private Integer getCustomerIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }
        String email = userDetails.getUsername();
        return customerRepository.findByUserEmail(email)
                .map(Customer::getCustomerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với email: " + email));
    }
}