package com.example.backend_fashionstore.shipping.controller;

import com.example.backend_fashionstore.shipping.dto.*;
import com.example.backend_fashionstore.shipping.service.ShippingService;
import com.example.backend_fashionstore.quanLyKhachHang.repository.KhachHangCustomerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {
    private final ShippingService shippingService;
    private final KhachHangCustomerRepository customerRepository;

    @PostMapping("/calculate")
    public ResponseEntity<ShippingCalculateResponse> calculate(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ShippingCalculateRequest request) {
        Integer customerId = customerRepository.findByUserEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng")).getCustomerId();
        return ResponseEntity.ok(shippingService.calculate(customerId, request));
    }
}
