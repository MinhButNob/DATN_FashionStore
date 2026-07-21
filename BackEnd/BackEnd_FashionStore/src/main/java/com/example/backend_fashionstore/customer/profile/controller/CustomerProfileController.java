package com.example.backend_fashionstore.customer.profile.controller;

import com.example.backend_fashionstore.customer.profile.dto.request.ChangePasswordRequestDTO;
import com.example.backend_fashionstore.customer.profile.dto.request.CustomerProfileRequestDTO;
import com.example.backend_fashionstore.customer.profile.dto.response.CustomerProfileResponseDTO;
import com.example.backend_fashionstore.customer.profile.service.CustomerProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping("/profile")
    public ResponseEntity<CustomerProfileResponseDTO> getProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        log.info("📥 Get profile for email: {}", email);
        CustomerProfileResponseDTO profile = customerProfileService.getProfile(email);
        log.info("📤 Profile data: phone={}, address={}", profile.getPhone(), profile.getAddress());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<CustomerProfileResponseDTO> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CustomerProfileRequestDTO request
    ) {
        String email = userDetails.getUsername();
        log.info("📝 Update profile for email: {}", email);
        log.info("📝 Request: phone={}, address={}", request.getPhone(), request.getAddress());

        CustomerProfileResponseDTO updated = customerProfileService.updateProfile(email, request);

        log.info("✅ Updated profile: phone={}, address={}", updated.getPhone(), updated.getAddress());
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordRequestDTO request
    ) {
        String email = userDetails.getUsername();
        log.info("🔐 Change password request for email: {}", email);

        try {
            customerProfileService.changePassword(email, request);
            log.info("✅ Password changed successfully for email: {}", email);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (RuntimeException e) {
            log.error("❌ Failed to change password for email {}: {}", email, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}