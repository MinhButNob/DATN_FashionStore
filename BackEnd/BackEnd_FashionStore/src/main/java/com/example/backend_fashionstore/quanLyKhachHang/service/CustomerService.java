package com.example.backend_fashionstore.quanLyKhachHang.service;

import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.enums.UserStatus;
import com.example.backend_fashionstore.quanLyKhachHang.dto.*;
import com.example.backend_fashionstore.quanLyKhachHang.repository.KhachHangCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final KhachHangCustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    // Lấy danh sách khách hàng (chỉ hiển thị khách hàng đang hoạt động)
    public Page<CustomerDTO> getAllCustomers(String keyword, Pageable pageable) {
        Page<Customer> customers;
        if (keyword != null && !keyword.isEmpty()) {
            customers = customerRepository.searchCustomers(keyword, pageable);
        } else {
            customers = customerRepository.findAll(pageable);
        }
        return customers.map(this::convertToDTO);
    }

    // Lấy chi tiết khách hàng theo ID
    public CustomerDTO getCustomerById(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        return convertToDTO(customer);
    }

    // Thêm khách hàng mới
    @Transactional
    public CustomerDTO createCustomer(CustomerCreateDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã tồn tại: " + dto.getEmail());
        }

        User user = User.builder()
                .username(dto.getEmail().split("@")[0])
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .provider("LOCAL")
                .status(UserStatus.ACTIVE)  // ✅ ĐÃ SỬA
                .build();
        user = userRepository.save(user);

        Customer customer = Customer.builder()
                .user(user)
                .customerName(dto.getCustomerName())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .customerStatus("ACTIVE")
                .build();
        customer = customerRepository.save(customer);

        return convertToDTO(customer);
    }

    // Cập nhật thông tin khách hàng
    @Transactional
    public CustomerDTO updateCustomer(Integer id, CustomerUpdateDTO dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        customer.setCustomerName(dto.getCustomerName());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());
        customer = customerRepository.save(customer);

        return convertToDTO(customer);
    }

    // Xóa khách hàng (soft delete - chuyển status thành DELETED)
    @Transactional
    public void deleteCustomer(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        User user = customer.getUser();
        user.setStatus(UserStatus.DELETED);  // ✅ ĐÃ SỬA
        user.setDeletedAt(LocalDateTime.now());  // ✅ THÊM deleted_at
        userRepository.save(user);
    }

    // Khôi phục khách hàng đã xóa
    @Transactional
    public void restoreCustomer(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        User user = customer.getUser();
        user.setStatus(UserStatus.ACTIVE);  // ✅ ĐÃ SỬA
        user.setDeletedAt(null);  // ✅ XÓA deleted_at
        userRepository.save(user);
    }

    // Khóa / Mở khóa tài khoản khách hàng
    @Transactional
    public void toggleLockCustomer(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        User user = customer.getUser();

        if (user.getStatus() == UserStatus.BANNED) {
            user.setStatus(UserStatus.ACTIVE);  // Mở khóa
        } else {
            user.setStatus(UserStatus.BANNED);  // Khóa
        }
        userRepository.save(user);
    }

    // Thống kê số lượng khách hàng
    public StatisticsDTO getStatistics() {
        long total = customerRepository.countAllActiveCustomers();
        long locked = customerRepository.countLockedCustomers();
        long active = customerRepository.countActiveCustomers();
        long categories = 0;

        return new StatisticsDTO(total, active, locked, categories);
    }

    // Chuyển đổi từ Entity sang DTO
    private CustomerDTO convertToDTO(Customer c) {
        CustomerDTO dto = new CustomerDTO();
        dto.setCustomerId(c.getCustomerId());
        dto.setUserId(c.getUser().getUserId());
        dto.setEmail(c.getUser().getEmail());
        dto.setUsername(c.getUser().getUsername());
        dto.setCustomerName(c.getCustomerName());
        dto.setPhone(c.getPhone());
        dto.setAddress(c.getAddress());
        dto.setIsLocked(c.getUser().getStatus() == UserStatus.BANNED);  // ✅ ĐÃ SỬA
        dto.setIsActive(c.getUser().getStatus() == UserStatus.ACTIVE);  // ✅ ĐÃ SỬA
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }
}