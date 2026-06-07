package com.example.backend_fashionstore.quanlyNhanVien.service;

import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.entity.Employee;
import com.example.backend_fashionstore.entity.User;
import com.example.backend_fashionstore.enums.UserStatus;
import com.example.backend_fashionstore.quanlyNhanVien.dto.EmployeeUpdateRequest;
import com.example.backend_fashionstore.quanlyNhanVien.repo.EmployeeRepository;
import com.example.backend_fashionstore.auth.repository.RoleRepository;
import com.example.backend_fashionstore.auth.repository.UserRoleRepository;
import com.example.backend_fashionstore.entity.Role;
import com.example.backend_fashionstore.entity.UserRole;
import com.example.backend_fashionstore.entity.UserRoleId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateServiceEmployee {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    public Employee update(Integer id, EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findByEmployeeId(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + id));

        // Lấy User từ employee
        User user = employee.getUser();
        if (user == null) {
            throw new RuntimeException("Nhân viên không có tài khoản liên kết");
        }

        // Cập nhật User nếu có username hoặc password
        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username đã tồn tại: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);

        // Kiểm tra số điện thoại (nếu thay đổi)
        if (!employee.getEmployeePhone().equals(request.getEmployeePhone())
                && employeeRepository.existsByEmployeePhoneAndEmployeeIdNot(
                request.getEmployeePhone(), id)) {
            throw new RuntimeException("Số điện thoại đã tồn tại: " + request.getEmployeePhone());
        }

        // Kiểm tra email (nếu thay đổi)
        if (!employee.getEmployeeEmail().equals(request.getEmployeeEmail())
                && employeeRepository.existsByEmployeeEmailAndEmployeeIdNot(
                request.getEmployeeEmail(), id)) {
            throw new RuntimeException("Email đã tồn tại: " + request.getEmployeeEmail());
        }

        employee.setEmployeeFullname(request.getEmployeeFullname());
        employee.setEmployeeGender(request.getEmployeeGender());
        employee.setEmployeePhone(request.getEmployeePhone());
        employee.setEmployeeEmail(request.getEmployeeEmail());
        // ❌ XÓA: employee.setEmployeeStatus(request.getEmployeeStatus());
        employee.setEmployeePosition(
                (request.getRoleName() != null && !request.getRoleName().isBlank())
                        ? request.getRoleName()
                        : ((request.getEmployeePosition() != null && !request.getEmployeePosition().isBlank())
                                ? request.getEmployeePosition()
                                : employee.getEmployeePosition())
        );

        // Cập nhật role
        if (request.getRoleName() != null && !request.getRoleName().isBlank()) {
            Role requestedRole = roleRepository.findByRoleName(request.getRoleName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy role: " + request.getRoleName()));
            
            userRoleRepository.deleteByUserId(user.getUserId());
            userRoleRepository.flush();
            
            UserRole userRole = UserRole.builder()
                    .id(new UserRoleId(user.getUserId(), requestedRole.getRoleId()))
                    .user(user)
                    .role(requestedRole)
                    .build();
            userRoleRepository.save(userRole);
        }

        return employeeRepository.save(employee);
    }

    // Khóa/Mở khóa tài khoản nhân viên
    @Transactional
    public void toggleLockEmployee(Integer id) {
        Employee employee = employeeRepository.findByEmployeeId(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        User user = employee.getUser();
        if (user == null) {
            throw new RuntimeException("Nhân viên không có tài khoản liên kết");
        }

        if (user.getStatus() == UserStatus.BANNED) {
            user.setStatus(UserStatus.ACTIVE);  // Mở khóa
        } else if (user.getStatus() == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.BANNED);   // Khóa
        } else {
            throw new RuntimeException("Không thể thao tác với nhân viên đã bị xóa");
        }
        userRepository.save(user);
    }

    // Xóa mềm nhân viên
    @Transactional
    public void softDeleteEmployee(Integer id) {
        Employee employee = employeeRepository.findByEmployeeId(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        User user = employee.getUser();
        if (user == null) {
            throw new RuntimeException("Nhân viên không có tài khoản liên kết");
        }

        if (user.getStatus() == UserStatus.DELETED) {
            throw new RuntimeException("Nhân viên đã bị xóa trước đó");
        }

        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    // Khôi phục nhân viên
    @Transactional
    public void restoreEmployee(Integer id) {
        Employee employee = employeeRepository.findByEmployeeId(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        User user = employee.getUser();
        if (user == null) {
            throw new RuntimeException("Nhân viên không có tài khoản liên kết");
        }

        if (user.getStatus() != UserStatus.DELETED) {
            throw new RuntimeException("Chỉ có thể khôi phục nhân viên đã bị xóa");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setDeletedAt(null);
        userRepository.save(user);
    }
}