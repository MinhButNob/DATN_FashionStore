package com.example.backend_fashionstore.auth.service;

import com.example.backend_fashionstore.entity.Role;
import com.example.backend_fashionstore.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    @Transactional
    public Role getOrCreateRole(String roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseGet(() -> {
                    try {
                        return roleRepository.save(Role.builder().roleName(roleName).build());
                    } catch (Exception e) {
                        // Nếu bị lỗi trùng, lấy role đã được tạo bởi thread khác
                        return roleRepository.findByRoleName(roleName)
                                .orElseThrow(() -> new RuntimeException("Không thể tạo role: " + roleName));
                    }
                });
    }
}