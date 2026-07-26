package com.example.backend_fashionstore.customer.cart.controller;

import com.example.backend_fashionstore.QuanLySanPham.dto.response.ApiResponse;
import com.example.backend_fashionstore.customer.cart.dto.request.CartRequestDTO;
import com.example.backend_fashionstore.customer.cart.dto.response.CartResponseDTO;
import com.example.backend_fashionstore.customer.cart.service.CartService;
import com.example.backend_fashionstore.customer.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CartRepository cartRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponseDTO>> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        CartResponseDTO cart = cartService.getCart(customerId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartResponseDTO>> addToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CartRequestDTO request
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        CartResponseDTO cart = cartService.addToCart(customerId, request);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponseDTO>> updateCartItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Integer cartItemId,
            @RequestParam Integer quantity
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        CartResponseDTO cart = cartService.updateCartItem(customerId, cartItemId, quantity);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponseDTO>> removeFromCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Integer cartItemId
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        CartResponseDTO cart = cartService.removeFromCart(customerId, cartItemId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        cartService.clearCart(customerId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ✅ API áp dụng voucher
    @PostMapping("/apply-voucher")
    public ResponseEntity<ApiResponse<CartResponseDTO>> applyVoucher(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String voucherCode
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        CartResponseDTO cart = cartService.applyVoucher(customerId, voucherCode);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    // ✅ API xóa voucher
    @DeleteMapping("/remove-voucher")
    public ResponseEntity<ApiResponse<CartResponseDTO>> removeVoucher(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        CartResponseDTO cart = cartService.removeVoucher(customerId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    private Integer getCustomerIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }
        String email = userDetails.getUsername();
        return cartRepository.findCustomerIdByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với email: " + email));
    }
}