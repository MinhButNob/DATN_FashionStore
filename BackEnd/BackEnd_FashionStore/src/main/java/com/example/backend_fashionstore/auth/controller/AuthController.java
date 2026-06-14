package com.example.backend_fashionstore.auth.controller;

import com.example.backend_fashionstore.auth.dto.*;
import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.auth.service.AuthService;
import com.example.backend_fashionstore.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;  // THÊM DÒNG NÀY
    private final PasswordEncoder passwordEncoder;  // THÊM DÒNG NÀY

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid
            @RequestBody RegisterRequest request
    ) {

        return ResponseEntity.ok(
                authService.register(request)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid
            @RequestBody LoginRequest request
    ) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request.getIdToken()));
    }

    @PostMapping("/facebook")
    public ResponseEntity<AuthResponse> facebookLogin(@RequestBody FacebookLoginRequest request) {
        return ResponseEntity.ok(authService.facebookLogin(request.getAccessToken()));
    }

    // ========== TEST ENDPOINTS (CHỈ DÙNG ĐỂ DEBUG) ==========

    @GetMapping("/test/hash")
    public ResponseEntity<String> hashPassword(@RequestParam String password) {
        String hashedPassword = passwordEncoder.encode(password);
        return ResponseEntity.ok("Hash for '" + password + "': " + hashedPassword);
    }

    @GetMapping("/test/check-password")
    public ResponseEntity<Map<String, Object>> checkPassword(
            @RequestParam String raw,
            @RequestParam String email) {

        Map<String, Object> result = new HashMap<>();

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            result.put("success", false);
            result.put("message", "User not found: " + email);
            return ResponseEntity.ok(result);
        }

        boolean matches = passwordEncoder.matches(raw, user.getPasswordHash());

        result.put("success", true);
        result.put("email", email);
        result.put("rawPassword", raw);
        result.put("passwordHash", user.getPasswordHash());
        result.put("matches", matches);

        if (matches) {
            result.put("message", "✅ Password is CORRECT!");
        } else {
            result.put("message", "❌ Password is WRONG!");
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/test/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
}