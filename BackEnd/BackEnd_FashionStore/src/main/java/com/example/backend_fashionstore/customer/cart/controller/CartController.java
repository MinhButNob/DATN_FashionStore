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
@CrossOrigin(origins = "http://localhost:5173")
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
        // Debug: In ra giá trị nhận được
        System.out.println("=== DEBUG ===");
        System.out.println("ProductDetailId: " + request.getProductDetailId());
        System.out.println("Quantity from request: " + request.getQuantity());
        System.out.println("Quantity getter: " + request.getQuantity());

        // Force set nếu null
        if (request.getQuantity() == null) {
            System.out.println("!!! Quantity is NULL, setting to 1 !!!");
            request.setQuantity(1);
        }

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

    private Integer getCustomerIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }

        String email = userDetails.getUsername();

        return cartRepository.findCustomerIdByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với email: " + email));
    }
}