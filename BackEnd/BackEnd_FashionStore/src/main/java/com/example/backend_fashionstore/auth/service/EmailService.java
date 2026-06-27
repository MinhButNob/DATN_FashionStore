// EmailService.java
package com.example.backend_fashionstore.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Mã OTP đặt lại mật khẩu - Fashion Store");
            message.setText(
                    "Kính chào quý khách,\n\n" +
                            "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản Fashion Store.\n\n" +
                            "Mã OTP của bạn là: " + otp + "\n\n" +
                            "Mã OTP có hiệu lực trong vòng 5 phút.\n\n" +
                            "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
                            "Trân trọng,\n" +
                            "Fashion Store"
            );
            mailSender.send(message);
            log.info("📧 OTP email sent to: {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send OTP email: {}", e.getMessage());
            throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }
}