package com.example.backend_fashionstore.security;

import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        CustomUserDetails details = CustomUserDetails.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(user.getUserRoles()
                        .stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRole().getRoleName()))
                        .collect(Collectors.toList()))
                .build();

        System.out.println("Locked = " + details.isAccountNonLocked());
        System.out.println("Enabled = " + details.isEnabled());
        System.out.println("Expired = " + details.isAccountNonExpired());

        return details;
    }
}

