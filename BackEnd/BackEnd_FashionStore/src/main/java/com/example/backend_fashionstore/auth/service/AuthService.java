package com.example.backend_fashionstore.auth.service;

import com.example.backend_fashionstore.auth.dto.AuthResponse;
import com.example.backend_fashionstore.auth.dto.LoginRequest;
import com.example.backend_fashionstore.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);
    AuthResponse googleLogin(String idToken);
    AuthResponse facebookLogin(String accessToken);

    AuthResponse login(LoginRequest request);
}

