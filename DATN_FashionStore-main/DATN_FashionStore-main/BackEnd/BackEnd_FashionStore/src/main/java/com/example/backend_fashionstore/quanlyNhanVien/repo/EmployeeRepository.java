package com.example.backend_fashionstore.quanlyNhanVien.repo;

import com.example.backend_fashionstore.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    @Query("SELECT e FROM Employee e WHERE " +
            "(:keyword IS NULL OR LOWER(e.employeeFullname) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.employeePhone) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.employeePosition) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.employeeEmail) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) AND " +
            "(:status IS NULL OR CAST(e.employeeStatus AS string) = CAST(:status AS string))")
    Page<Employee> searchEmployees(@Param("keyword") String keyword,
                                   @Param("status") String status,
                                   Pageable pageable);

    Optional<Employee> findByEmployeeId(Integer employeeId);

    // Kiểm tra tồn tại khi tạo mới
    boolean existsByEmployeePhone(String employeePhone);
    boolean existsByEmployeeEmail(String employeeEmail);

    // Kiểm tra tồn tại khi cập nhật (bỏ qua chính nó)
    @Query("SELECT COUNT(e) > 0 FROM Employee e WHERE e.employeePhone = :phone AND e.employeeId != :id")
    boolean existsByEmployeePhoneAndEmployeeIdNot(@Param("phone") String phone, @Param("id") Integer id);

    @Query("SELECT COUNT(e) > 0 FROM Employee e WHERE e.employeeEmail = :email AND e.employeeId != :id")
    boolean existsByEmployeeEmailAndEmployeeIdNot(@Param("email") String email, @Param("id") Integer id);

    // Thống kê
    @Query("SELECT COUNT(e) FROM Employee e")
    long countTotalEmployees();

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.employeeStatus = 'ACTIVE'")
    long countActiveEmployees();
}