package com.example.backend_fashionstore.quanlyNhanVien.service;

import com.example.backend_fashionstore.auth.repository.RoleRepository;
import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.auth.repository.UserRoleRepository;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.enums.UserStatus;  // ✅ THÊM IMPORT
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

        // 3. Tạo User - ĐÃ SỬA
        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmployeeEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .provider("LOCAL")
                .status(UserStatus.ACTIVE)  // ✅ ĐÃ SỬA
                .build();
        User savedUser = userRepository.save(newUser);

        // 4. Lấy hoặc tạo role STAFF
        Role staffRole = roleRepository.findByRoleName("STAFF")
                .orElseGet(() -> roleRepository.save(
                        Role.builder().roleName("STAFF").build()
                ));

        // 5. Gán role STAFF cho user (mặc định nhân viên là STAFF)
        boolean hasRole = userRoleRepository
                .existsByUser_UserIdAndRole_RoleName(savedUser.getUserId(), "STAFF");
        if (!hasRole) {
            UserRole userRole = UserRole.builder()
                    .id(new UserRoleId(savedUser.getUserId(), staffRole.getRoleId()))
                    .user(savedUser)
                    .role(staffRole)
                    .build();
            userRoleRepository.save(userRole);
        }

        // 6. Tạo Employee
        Employee employee = new Employee();
        employee.setUserId(savedUser.getUserId());
        employee.setEmployeeFullname(request.getEmployeeFullname());
        employee.setEmployeeGender(request.getEmployeeGender());
        employee.setEmployeeAge(request.getEmployeeAge());
        employee.setEmployeePhone(request.getEmployeePhone());
        employee.setEmployeeEmail(request.getEmployeeEmail());
        employee.setEmployeeStatus(
                request.getEmployeeStatus() != null ? request.getEmployeeStatus() : "ACTIVE"
        );
        // ✅ Mặc định chức vụ là "Nhân viên" nếu FE không truyền
        employee.setEmployeePosition(
                (request.getEmployeePosition() != null && !request.getEmployeePosition().isBlank())
                        ? request.getEmployeePosition()
                        : "Nhân viên"
        );

        return employeeRepository.save(employee);
    }
}