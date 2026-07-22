package com.example.backend_fashionstore.customer.profile.service;

import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.customer.profile.dto.request.ChangePasswordRequestDTO;
import com.example.backend_fashionstore.customer.profile.dto.request.CustomerProfileRequestDTO;
import com.example.backend_fashionstore.customer.profile.dto.response.CustomerProfileResponseDTO;
import com.example.backend_fashionstore.customer.profile.repository.CustomerProfileRepository;
import com.example.backend_fashionstore.entity.Customer;
import com.example.backend_fashionstore.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Lấy thông tin profile của khách hàng theo email
     */
    public CustomerProfileResponseDTO getProfile(String email) {
        log.info("🔍 Getting profile for email: {}", email);

        Customer customer = customerProfileRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    log.info("📝 Customer not found, creating new for email: {}", email);
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

                    Customer newCustomer = Customer.builder()
                            .user(user)
                            .customerName(user.getUsername())
                            .build();
                    return customerProfileRepository.save(newCustomer);
                });

        log.info("✅ Found customer: id={}, name={}, phone={}, address={}",
                customer.getCustomerId(),
                customer.getCustomerName(),
                customer.getPhone(),
                customer.getAddress());

        return convertToDTO(customer);
    }

    /**
     * Cập nhật thông tin profile của khách hàng
     */
    @Transactional
    public CustomerProfileResponseDTO updateProfile(String email, CustomerProfileRequestDTO request) {
        log.info("🔄 Updating profile for email: {}", email);
        log.info("📝 Request data: fullName={}, phone={}, address={}",
                request.getFullName(),
                request.getPhone(),
                request.getAddress());

        Customer customer = customerProfileRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    log.info("📝 Customer not found, creating new for email: {}", email);
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

                    Customer newCustomer = Customer.builder()
                            .user(user)
                            .customerName(user.getUsername())
                            .build();
                    return customerProfileRepository.save(newCustomer);
                });

        User user = customer.getUser();

        // Cập nhật tên
        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            user.setUsername(request.getFullName());
            customer.setCustomerName(request.getFullName());
            log.info("✅ Updated name to: {}", request.getFullName());
        }

        // Cập nhật email
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            user.setEmail(request.getEmail());
            log.info("✅ Updated email to: {}", request.getEmail());
        }

        // ✅ Cập nhật số điện thoại - QUAN TRỌNG
        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            if (!phone.isEmpty()) {
                customer.setPhone(phone);
                log.info("✅ Updated phone to: {}", phone);
            } else {
                log.warn("⚠️ Phone is empty string, keeping existing: {}", customer.getPhone());
            }
        } else {
            log.warn("⚠️ Phone is null, keeping existing: {}", customer.getPhone());
        }

        // Cập nhật địa chỉ
        if (request.getAddress() != null) {
            customer.setAddress(request.getAddress());
            log.info("✅ Updated address to: {}", request.getAddress());
        }

        // Lưu thay đổi
        userRepository.save(user);
        Customer updatedCustomer = customerProfileRepository.save(customer);

        log.info("✅ Profile updated successfully!");
        log.info("📋 Final data: name={}, phone={}, address={}",
                updatedCustomer.getCustomerName(),
                updatedCustomer.getPhone(),
                updatedCustomer.getAddress());

        return convertToDTO(updatedCustomer);
    }

    /**
     * Đổi mật khẩu
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequestDTO request) {
        log.info("🔐 Changing password for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("❌ Current password is incorrect for email: {}", email);
            throw new RuntimeException("Mật khẩu hiện tại không chính xác");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            log.warn("❌ New password is same as old password for email: {}", email);
            throw new RuntimeException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("✅ Password changed successfully for email: {}", email);
    }

    /**
     * Chuyển đổi Customer entity sang DTO - ✅ TRẢ VỀ PHONE
     */
    private CustomerProfileResponseDTO convertToDTO(Customer customer) {
        return CustomerProfileResponseDTO.builder()
                .customerId(customer.getCustomerId())
                .fullName(customer.getCustomerName())
                .email(customer.getUser().getEmail())
                .phone(customer.getPhone())  // ✅ TRẢ VỀ SỐ ĐIỆN THOẠI ĐÃ LƯU
                .address(customer.getAddress())
                .birthday(null)
                .gender(null)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}