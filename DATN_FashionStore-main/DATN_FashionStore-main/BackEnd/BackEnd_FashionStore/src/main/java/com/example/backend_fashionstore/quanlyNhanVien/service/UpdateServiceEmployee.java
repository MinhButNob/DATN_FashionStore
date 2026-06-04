package com.example.backend_fashionstore.quanlyNhanVien.service;

import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.entity.Employee;
import com.example.backend_fashionstore.entity.User;
import com.example.backend_fashionstore.quanlyNhanVien.dto.EmployeeUpdateRequest;
import com.example.backend_fashionstore.quanlyNhanVien.repo.EmployeeRepository;
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

    @Transactional
    public Employee update(Integer id, EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findByEmployeeId(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + id));

        // ✅ Cập nhật User nếu có username hoặc password và employee có liên kết user
        if ((request.getUsername() != null || request.getPassword() != null)
                && employee.getUserId() != null) {

            User user = userRepository.findById(employee.getUserId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user tương ứng"));

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
        }

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
        employee.setEmployeeAge(request.getEmployeeAge());
        employee.setEmployeePhone(request.getEmployeePhone());
        employee.setEmployeeEmail(request.getEmployeeEmail());
        employee.setEmployeeStatus(request.getEmployeeStatus());
        // ✅ Giữ chức vụ cũ nếu FE không truyền
        employee.setEmployeePosition(
                (request.getEmployeePosition() != null && !request.getEmployeePosition().isBlank())
                        ? request.getEmployeePosition()
                        : employee.getEmployeePosition()
        );

        return employeeRepository.save(employee);
    }
}