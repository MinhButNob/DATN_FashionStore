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
import java.util.List;
import java.util.Optional;

@Repository
public interface KhachHangCustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByUserUserId(Integer userId);
    Optional<Customer> findByUserEmail(String email);

    // ✅ SỬA: Tìm theo phone - trả về Optional (nếu có nhiều thì lấy cái đầu tiên)
    @Query("SELECT c FROM Customer c WHERE c.phone = :phone AND c.user.status <> 'DELETED'")
    Optional<Customer> findByPhone(@Param("phone") String phone);

    // ✅ THÊM MỚI: Tìm tất cả theo phone
    @Query("SELECT c FROM Customer c WHERE c.phone = :phone AND c.user.status <> 'DELETED'")
    List<Customer> findAllByPhone(@Param("phone") String phone);

    // ✅ Kiểm tra tồn tại theo phone
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.phone = :phone AND c.user.status <> 'DELETED'")
    boolean existsByPhone(@Param("phone") String phone);

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

    @Query("SELECT c FROM Customer c WHERE c.user.status <> 'DELETED'")
    Page<Customer> findAllActiveCustomers(Pageable pageable);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.user.status = :status")
    long countByUserStatus(@Param("status") UserStatus status);

    // ===== DASHBOARD =====
    @Query("SELECT COUNT(c) FROM Customer c")
    long countAllCustomers();

    @Query("SELECT c FROM Customer c WHERE c.user.status = 'DELETED'")
    Page<Customer> findAllDeletedCustomers(Pageable pageable);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.user.status <> 'DELETED' " +
            "AND EXTRACT(YEAR FROM c.createdAt) = :year " +
            "AND EXTRACT(MONTH FROM c.createdAt) = :month")
    long countNewCustomersByMonth(@Param("year") int year, @Param("month") int month);

    @Query(value = "SELECT COUNT(*) FROM customers c " +
            "JOIN users u ON c.customer_user_id = u.user_id " +
            "WHERE u.user_status <> 'DELETED' " +
            "AND DATE(c.customer_created_at) = CURRENT_DATE", nativeQuery = true)
    long countNewCustomersToday();

    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "c.phone LIKE CONCAT('%', :keyword, '%')")
    Page<Customer> searchAllCustomers(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM customers c " +
            "JOIN users u ON c.customer_user_id = u.user_id " +
            "WHERE u.user_status <> 'DELETED' " +
            "AND DATE(c.customer_created_at) = DATE(:date)", nativeQuery = true)
    long countNewCustomersByDate(@Param("date") LocalDate date);

    // ✅ SỬA: Tìm kiếm khách hàng trả về List
    @Query("SELECT c FROM Customer c WHERE c.user.status <> 'DELETED' " +
            "AND (LOWER(c.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR c.phone LIKE CONCAT('%', :keyword, '%'))")
    List<Customer> searchCustomers(@Param("keyword") String keyword);

    @Query("SELECT c FROM Customer c WHERE c.user.status <> 'DELETED'")
    List<Customer> findAllActiveCustomers();
}