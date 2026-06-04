package com.example.backend_fashionstore.auth.controller;

import com.example.backend_fashionstore.auth.dto.UserProfileResponse;
import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public UserProfileResponse me(
            Authentication authentication
    ) {
        String email =
                authentication.getName();

        User user =
                userRepository.findByEmailWithRoles(email)
                        .orElseThrow();

        List<String> roles =
                user.getUserRoles()
                        .stream()
                        .map(role ->
                                role.getRole()
                                        .getRoleName()
                        )
                        .toList();

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .roles(roles)
                .build();
    }
}