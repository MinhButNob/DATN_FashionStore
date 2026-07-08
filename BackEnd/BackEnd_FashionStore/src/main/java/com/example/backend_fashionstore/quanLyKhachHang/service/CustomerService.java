package com.example.backend_fashionstore.quanLyKhachHang.service;

import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.enums.UserStatus;
import com.example.backend_fashionstore.quanLyKhachHang.dto.*;
import com.example.backend_fashionstore.quanLyKhachHang.repository.KhachHangCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final KhachHangCustomerRepository customerRepository;
    private final UserRepository userRepository;

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

        // Kiểm tra nếu customer đã bị xóa mềm
        if (customer.getUser().getStatus() == UserStatus.DELETED) {
            throw new RuntimeException("Khách hàng đã bị xóa");
        }

        // ✅ Đảm bảo tài khoản này thực sự có role CUSTOMER (đồng nhất với danh sách)
        boolean isCustomerRole = customer.getUser().getUserRoles() != null &&
                customer.getUser().getUserRoles().stream()
                        .anyMatch(ur -> "CUSTOMER".equals(ur.getRole().getRoleName()));
        if (!isCustomerRole) {
            throw new RuntimeException("Tài khoản này không có role khách hàng");
        }

        return convertToDTO(customer);
    }

    // Xóa khách hàng (soft delete - chuyển status thành DELETED)
    @Transactional
    public void deleteCustomer(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        User user = customer.getUser();

        // Không cho xóa nếu đã xóa rồi
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

    // Khóa / Mở khóa tài khoản khách hàng (BANNED)
    @Transactional
    public void toggleLockCustomer(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        User user = customer.getUser();

        // Không cho khóa nếu đã xóa
        if (user.getStatus() == UserStatus.DELETED) {
            throw new RuntimeException("Không thể khóa khách hàng đã bị xóa");
        }

        if (user.getStatus() == UserStatus.BANNED) {
            user.setStatus(UserStatus.ACTIVE);  // Mở khóa
        } else {
            user.setStatus(UserStatus.BANNED);   // Khóa
        }
        userRepository.save(user);
    }

    // Thống kê số lượng khách hàng
    public StatisticsDTO getStatistics() {
        long total = customerRepository.countAllCustomers();           // Tổng số khách (kể cả đã xóa)
        long active = customerRepository.countByUserStatus(UserStatus.ACTIVE);      // Đang hoạt động
        long locked = customerRepository.countByUserStatus(UserStatus.BANNED);      // Bị khóa
        long deleted = customerRepository.countByUserStatus(UserStatus.DELETED);    // Đã xóa

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