package com.example.backend_fashionstore.quanlyNhanVien.service;

import com.example.backend_fashionstore.auth.repository.RoleRepository;
import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.auth.repository.UserRoleRepository;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.enums.RoleStatus;
import com.example.backend_fashionstore.enums.UserStatus;
import com.example.backend_fashionstore.quanlyNhanVien.dto.EmployeeCreateRequest;
import com.example.backend_fashionstore.quanlyNhanVien.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaveServiceEmployee {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Employee save(EmployeeCreateRequest request) {

        // 1. Kiểm tra trùng nhân viên
        if (employeeRepository.existsByEmployeePhone(request.getEmployeePhone())) {
            throw new RuntimeException("Số điện thoại đã tồn tại: " + request.getEmployeePhone());
        }
        if (employeeRepository.existsByEmployeeEmail(request.getEmployeeEmail())) {
            throw new RuntimeException("Email nhân viên đã tồn tại: " + request.getEmployeeEmail());
        }

        // 2. Kiểm tra trùng user
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username đã tồn tại: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmployeeEmail())) {
            throw new RuntimeException("Email user đã tồn tại: " + request.getEmployeeEmail());
        }

        // 3. Tạo User
        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmployeeEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .provider("LOCAL")
                .status(UserStatus.ACTIVE)
                .build();
        User savedUser = userRepository.save(newUser);

        // 4. Lấy role được chọn (nếu không có thì STAFF)
        String roleName = (request.getRoleName() != null && !request.getRoleName().isBlank()) 
                ? request.getRoleName() : "STAFF";

        Role requestedRole = roleRepository.findByRoleName(roleName)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .roleName(roleName)
                                .status(RoleStatus.ACTIVE)
                                .build()
                ));

        // 5. Gán role cho user
        boolean hasRole = userRoleRepository
                .existsByUser_UserIdAndRole_RoleName(savedUser.getUserId(), roleName);
        if (!hasRole) {
            UserRole userRole = UserRole.builder()
                    .id(new UserRoleId(savedUser.getUserId(), requestedRole.getRoleId()))
                    .user(savedUser)
                    .role(requestedRole)
                    .build();
            userRoleRepository.save(userRole);
        }

        // 6. Tạo Employee - ✅ SỬA: dùng user thay vì userId
        Employee employee = Employee.builder()
                .user(savedUser)  // ✅ Set user object
                .employeeFullname(request.getEmployeeFullname())
                .employeeGender(request.getEmployeeGender())
                .employeePhone(request.getEmployeePhone())
                .employeeEmail(request.getEmployeeEmail())
                .employeeStatus("ACTIVE")
                .employeePosition(request.getRoleName() != null ? request.getRoleName() : (request.getEmployeePosition() != null ? request.getEmployeePosition() : "Nhân viên"))
                .build();

        return employeeRepository.save(employee);
    }
}