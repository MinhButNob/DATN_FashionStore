// ForgotPasswordController.java
package com.example.backend_fashionstore.auth.controller;

import com.example.backend_fashionstore.auth.dto.ForgotPasswordRequestDTO;
import com.example.backend_fashionstore.auth.dto.ResetPasswordRequestDTO;
import com.example.backend_fashionstore.auth.dto.VerifyOtpRequestDTO;
import com.example.backend_fashionstore.auth.service.ForgotPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    /**
     * 1. Gửi OTP qua email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request
    ) {
        log.info("📤 Forgot password request for: {}", request.getEmail());
        forgotPasswordService.sendOtp(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư.");
        response.put("email", request.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * 2. Xác thực OTP
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequestDTO request
    ) {
        log.info("🔍 Verifying OTP for: {}", request.getEmail());
        boolean isValid = forgotPasswordService.verifyOtp(request);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        response.put("message", "Mã OTP hợp lệ");
        response.put("email", request.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * 3. Đặt lại mật khẩu
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request
    ) {
        log.info("🔄 Resetting password for: {}", request.getEmail());
        forgotPasswordService.resetPassword(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại.");
        return ResponseEntity.ok(response);
    }
}