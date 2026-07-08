package com.example.backend_fashionstore.quanLyKhachHang.repository;

import com.example.backend_fashionstore.entity.Customer;
import com.example.backend_fashionstore.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhachHangCustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByUserUserId(Integer userId);
    Optional<Customer> findByUserEmail(String email);

    // ✅ Tìm kiếm khách hàng (kể cả đã xóa? tùy logic) - chỉ lấy user có role CUSTOMER
    @Query("SELECT DISTINCT c FROM Customer c JOIN c.user.userRoles ur WHERE " +
            "ur.role.roleName = 'CUSTOMER' AND c.user.status <> 'DELETED' AND (" +
            "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "c.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<Customer> searchCustomers(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Customer c JOIN c.user.userRoles ur WHERE " +
            "ur.role.roleName = 'CUSTOMER' AND c.user.status = :status AND (" +
            "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "c.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<Customer> searchCustomersByStatus(@Param("keyword") String keyword, @Param("status") UserStatus status, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Customer c JOIN c.user.userRoles ur WHERE " +
            "ur.role.roleName = 'CUSTOMER' AND c.user.status = :status")
    Page<Customer> findAllByStatus(@Param("status") UserStatus status, Pageable pageable);

    // ✅ Lấy tất cả khách hàng chưa xóa - chỉ role CUSTOMER
    @Query("SELECT DISTINCT c FROM Customer c JOIN c.user.userRoles ur WHERE " +
            "ur.role.roleName = 'CUSTOMER' AND c.user.status <> 'DELETED'")
    Page<Customer> findAllActiveCustomers(Pageable pageable);

    // ✅ Đếm theo status của user - chỉ role CUSTOMER
    @Query("SELECT COUNT(DISTINCT c) FROM Customer c JOIN c.user.userRoles ur WHERE " +
            "ur.role.roleName = 'CUSTOMER' AND c.user.status = :status")
    long countByUserStatus(@Param("status") UserStatus status);

    // ✅ Tổng số khách hàng (kể cả đã xóa) - chỉ role CUSTOMER
    @Query("SELECT COUNT(DISTINCT c) FROM Customer c JOIN c.user.userRoles ur WHERE ur.role.roleName = 'CUSTOMER'")
    long countAllCustomers();
}