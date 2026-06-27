// ForgotPasswordService.java
package com.example.backend_fashionstore.auth.service;

import com.example.backend_fashionstore.auth.dto.ForgotPasswordRequestDTO;
import com.example.backend_fashionstore.auth.dto.ResetPasswordRequestDTO;
import com.example.backend_fashionstore.auth.dto.VerifyOtpRequestDTO;
import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.entity.PasswordResetToken;
import com.example.backend_fashionstore.entity.User;
import com.example.backend_fashionstore.auth.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final int OTP_EXPIRY_MINUTES = 5;

    /**
     * Gửi OTP qua email
     */
    @Transactional
    public void sendOtp(ForgotPasswordRequestDTO request) {
        String email = request.getEmail();
        log.info("📤 Sending OTP to: {}", email);

        // Kiểm tra email tồn tại
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));

        // Xóa OTP cũ
        tokenRepository.deleteByEmail(email);

        // Tạo OTP mới (6 số)
        String otp = generateOtp();

        // Lưu vào database
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .otp(otp)
                .expiryDate(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .isUsed(false)
                .createdAt(LocalDateTime.now())
                .build();
        tokenRepository.save(token);

        // Gửi email
        emailService.sendOtpEmail(email, otp);

        log.info("✅ OTP sent successfully to: {}", email);
    }

    /**
     * Xác thực OTP
     */
    public boolean verifyOtp(VerifyOtpRequestDTO request) {
        log.info("🔍 Verifying OTP for: {}", request.getEmail());

        PasswordResetToken token = tokenRepository
                .findByEmailAndOtpAndIsUsedFalse(request.getEmail(), request.getOtp())
                .orElseThrow(() -> new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn"));

        // Kiểm tra OTP đã hết hạn
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã OTP đã hết hạn. Vui lòng yêu cầu lại.");
        }

        log.info("✅ OTP verified successfully for: {}", request.getEmail());
        return true;
    }

    /**
     * Đặt lại mật khẩu
     */
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        log.info("🔄 Resetting password for: {}", request.getEmail());

        // Xác thực OTP
        PasswordResetToken token = tokenRepository
                .findByEmailAndOtpAndIsUsedFalse(request.getEmail(), request.getOtp())
                .orElseThrow(() -> new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn"));

        // Kiểm tra OTP đã hết hạn
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã OTP đã hết hạn. Vui lòng yêu cầu lại.");
        }

        // Lấy user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Cập nhật mật khẩu
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Đánh dấu OTP đã sử dụng
        tokenRepository.markAsUsed(request.getEmail(), request.getOtp());

        log.info("✅ Password reset successfully for: {}", request.getEmail());
    }

    /**
     * Tạo OTP ngẫu nhiên 6 số
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}