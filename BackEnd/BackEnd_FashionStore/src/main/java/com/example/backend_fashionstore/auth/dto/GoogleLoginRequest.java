package com.example.backend_fashionstore.auth.dto;

import lombok.Data;

@Data
public class GoogleLoginRequest {
    private String idToken;
}