package com.example.backend_fashionstore.quanLyKhachHang.service;

import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.enums.UserStatus;
import com.example.backend_fashionstore.quanLyKhachHang.dto.*;
import com.example.backend_fashionstore.quanLyKhachHang.repository.KhachHangCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final KhachHangCustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ✅ BỎ CHECK TRÙNG SĐT - Luôn trả về false
    public boolean isPhoneExists(String phone) {
        return false;
    }

    // ✅ Tìm khách hàng theo số điện thoại (trả về 1 khách hàng)
    public CustomerDTO getCustomerByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }
        Customer customer = customerRepository.findByPhone(phone.trim())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với số điện thoại: " + phone));
        return convertToDTO(customer);
    }

    // ✅ THÊM MỚI: Tìm tất cả khách hàng theo số điện thoại
    public List<CustomerDTO> findAllByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return List.of();
        }
        List<Customer> customers = customerRepository.findAllByPhone(phone.trim());
        return customers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ✅ Tạo khách hàng mới
    @Transactional
    public CustomerDTO createCustomer(CustomerCreateDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email '" + dto.getEmail() + "' đã được sử dụng");
        }

        try {
            User user = User.builder()
                    .username(dto.getEmail())
                    .email(dto.getEmail())
                    .passwordHash(passwordEncoder.encode(dto.getPassword()))
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            user = userRepository.save(user);

            Customer customer = Customer.builder()
                    .user(user)
                    .customerName(dto.getCustomerName())
                    .phone(dto.getPhone())
                    .address(dto.getAddress())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            customer = customerRepository.save(customer);

            return convertToDTO(customer);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Không thể tạo khách hàng. Vui lòng thử lại.");
        }
    }

    // ✅ Cập nhật khách hàng
    @Transactional
    public CustomerDTO updateCustomer(Integer id, CustomerUpdateDTO dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        if (customer.getUser().getStatus() == UserStatus.DELETED) {
            throw new RuntimeException("Khách hàng đã bị xóa, không thể cập nhật");
        }

        customer.setPhone(dto.getPhone());
        customer.setCustomerName(dto.getCustomerName());
        customer.setAddress(dto.getAddress());
        customer.setUpdatedAt(LocalDateTime.now());

        customer = customerRepository.save(customer);
        return convertToDTO(customer);
    }

    // ✅ Tạo hoặc tìm khách hàng theo SĐT (dùng cho POS)
    @Transactional
    public Customer findOrCreateCustomerByPhone(String customerName, String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }

        return customerRepository.findByPhone(phone.trim())
                .orElseGet(() -> {
                    String username = "customer_" + System.currentTimeMillis();
                    String email = "customer_" + System.currentTimeMillis() + "@temp.com";

                    User user = User.builder()
                            .username(username)
                            .email(email)
                            .passwordHash(passwordEncoder.encode("123456"))
                            .status(UserStatus.ACTIVE)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    user = userRepository.save(user);

                    Customer customer = Customer.builder()
                            .user(user)
                            .customerName(customerName != null ? customerName : "Khách lẻ")
                            .phone(phone.trim())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return customerRepository.save(customer);
                });
    }

    // Lấy danh sách khách hàng (có hỗ trợ filter)
    public Page<CustomerDTO> getAllCustomers(String keyword, String statusParam, Pageable pageable) {
        UserStatus status = null;
        if (statusParam != null && !statusParam.isEmpty()) {
            if (statusParam.equalsIgnoreCase("ACTIVE")) status = UserStatus.ACTIVE;
            else if (statusParam.equalsIgnoreCase("LOCKED")) status = UserStatus.BANNED;
            else if (statusParam.equalsIgnoreCase("DELETED")) status = UserStatus.DELETED;
        }

        Page<Customer> customers;
        boolean hasKeyword = (keyword != null && !keyword.isEmpty());

        if (status != null) {
            if (hasKeyword) {
                customers = customerRepository.searchCustomersByStatus(keyword, status, pageable);
            } else {
                customers = customerRepository.findAllByStatus(status, pageable);
            }
        } else {
            if (hasKeyword) {
                customers = customerRepository.searchCustomers(keyword, pageable);
            } else {
                customers = customerRepository.findAllActiveCustomers(pageable);
            }
        }
        return customers.map(this::convertToDTO);
    }

    // Lấy chi tiết khách hàng theo ID
    public CustomerDTO getCustomerById(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        if (customer.getUser().getStatus() == UserStatus.DELETED) {
            throw new RuntimeException("Khách hàng đã bị xóa");
        }

        boolean isCustomerRole = customer.getUser().getUserRoles() != null &&
                customer.getUser().getUserRoles().stream()
                        .anyMatch(ur -> "CUSTOMER".equals(ur.getRole().getRoleName()));
        if (!isCustomerRole) {
            throw new RuntimeException("Tài khoản này không có role khách hàng");
        }

        return convertToDTO(customer);
    }

    // Xóa khách hàng (soft delete)
    @Transactional
    public void deleteCustomer(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        User user = customer.getUser();

        if (user.getStatus() == UserStatus.DELETED) {
            throw new RuntimeException("Khách hàng đã bị xóa trước đó");
        }

        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // Khôi phục khách hàng đã xóa
    @Transactional
    public void restoreCustomer(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        User user = customer.getUser();

        if (user.getStatus() != UserStatus.DELETED) {
            throw new RuntimeException("Chỉ có thể khôi phục khách hàng đã bị xóa");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setDeletedAt(null);
        userRepository.save(user);
    }

    // Khóa / Mở khóa tài khoản khách hàng
    @Transactional
    public void toggleLockCustomer(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        User user = customer.getUser();

        if (user.getStatus() == UserStatus.DELETED) {
            throw new RuntimeException("Không thể khóa khách hàng đã bị xóa");
        }

        if (user.getStatus() == UserStatus.BANNED) {
            user.setStatus(UserStatus.ACTIVE);
        } else {
            user.setStatus(UserStatus.BANNED);
        }
        userRepository.save(user);
    }

    // Thống kê số lượng khách hàng
    public StatisticsDTO getStatistics() {
        long total = customerRepository.countAllCustomers();
        long active = customerRepository.countByUserStatus(UserStatus.ACTIVE);
        long locked = customerRepository.countByUserStatus(UserStatus.BANNED);
        long deleted = customerRepository.countByUserStatus(UserStatus.DELETED);

        return new StatisticsDTO(total, active, locked, deleted);
    }

    // Chuyển đổi từ Entity sang DTO
    private CustomerDTO convertToDTO(Customer c) {
        User user = c.getUser();

        CustomerDTO dto = new CustomerDTO();
        dto.setCustomerId(c.getCustomerId());
        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setCustomerName(c.getCustomerName());
        dto.setPhone(c.getPhone());
        dto.setAddress(c.getAddress());
        dto.setIsLocked(user.getStatus() == UserStatus.BANNED);
        dto.setIsActive(user.getStatus() == UserStatus.ACTIVE);
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }
}