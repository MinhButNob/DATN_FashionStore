package com.example.backend_fashionstore.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private UserResponse user;
    private Integer customerId;  // ✅ THÊM
    private String phone;        // ✅ THÊM
}