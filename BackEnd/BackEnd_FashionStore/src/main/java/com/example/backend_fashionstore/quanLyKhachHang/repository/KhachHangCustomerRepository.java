package com.example.backend_fashionstore.quanLyKhachHang.repository;

import com.example.backend_fashionstore.entity.Customer;
import com.example.backend_fashionstore.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface KhachHangCustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByUserUserId(Integer userId);
    Optional<Customer> findByUserEmail(String email);

    // ✅ Tìm kiếm khách hàng (kể cả đã xóa? tùy logic)
    @Query("SELECT c FROM Customer c WHERE " +
            "c.user.status <> 'DELETED' AND (" +
            "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "c.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<Customer> searchCustomers(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.user.status = :status AND (" +
            "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "c.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<Customer> searchCustomersByStatus(@Param("keyword") String keyword, @Param("status") UserStatus status, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.user.status = :status")
    Page<Customer> findAllByStatus(@Param("status") UserStatus status, Pageable pageable);

    // ✅ Lấy tất cả khách hàng chưa xóa
    @Query("SELECT c FROM Customer c WHERE c.user.status <> 'DELETED'")
    Page<Customer> findAllActiveCustomers(Pageable pageable);

    // ✅ Đếm theo status của user
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.user.status = :status")
    long countByUserStatus(@Param("status") UserStatus status);

    // ===== DASHBOARD - THÊM MỚI =====

    /**
     * Đếm tổng số khách hàng (bao gồm cả đã xóa)
     */
    @Query("SELECT COUNT(c) FROM Customer c")
    long countAllCustomers();

    /**
     * Đếm khách hàng theo trạng thái (có thể truyền vào status)
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.user.status = :status")
    long countCustomersByStatus(@Param("status") UserStatus status);

    /**
     * Lấy danh sách khách hàng đã xóa
     */
    @Query("SELECT c FROM Customer c WHERE c.user.status = 'DELETED'")
    Page<Customer> findAllDeletedCustomers(Pageable pageable);

    /**
     * Đếm số khách hàng mới trong tháng
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.user.status <> 'DELETED' " +
            "AND EXTRACT(YEAR FROM c.createdAt) = :year " +
            "AND EXTRACT(MONTH FROM c.createdAt) = :month")
    long countNewCustomersByMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Đếm số khách hàng mới trong ngày
     * ✅ SỬA: Dùng native query để so sánh DATE
     */
    @Query(value = "SELECT COUNT(*) FROM customers c " +
            "JOIN users u ON c.customer_user_id = u.user_id " +
            "WHERE u.user_status <> 'DELETED' " +
            "AND DATE(c.customer_created_at) = CURRENT_DATE", nativeQuery = true)
    long countNewCustomersToday();

    /**
     * Tìm kiếm khách hàng (bao gồm cả đã xóa)
     */
    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "c.phone LIKE CONCAT('%', :keyword, '%')")
    Page<Customer> searchAllCustomers(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Lấy khách hàng theo số điện thoại
     */
    Optional<Customer> findByPhone(String phone);

    /**
     * ✅ THÊM MỚI: Đếm khách hàng mới theo ngày cụ thể
     */
    @Query(value = "SELECT COUNT(*) FROM customers c " +
            "JOIN users u ON c.customer_user_id = u.user_id " +
            "WHERE u.user_status <> 'DELETED' " +
            "AND DATE(c.customer_created_at) = DATE(:date)", nativeQuery = true)
    long countNewCustomersByDate(@Param("date") LocalDate date);
}