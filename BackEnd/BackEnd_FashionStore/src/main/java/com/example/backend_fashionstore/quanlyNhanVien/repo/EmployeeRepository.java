package com.example.backend_fashionstore.quanlyNhanVien.repo;

import com.example.backend_fashionstore.entity.Employee;
import com.example.backend_fashionstore.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    // ✅ Tìm kiếm nhân viên (có lọc theo status của User)
    @Query("SELECT e FROM Employee e WHERE " +
            "(:keyword IS NULL OR " +
            "LOWER(e.employeeFullname) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.employeePhone) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.employeePosition) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.employeeEmail) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.user.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.user.username) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) AND " +
            "(:status IS NULL OR e.employeeStatus = :status)")
    Page<Employee> searchEmployees(@Param("keyword") String keyword,
                                   @Param("status") String status,
                                   Pageable pageable);

    // ✅ Tìm kiếm theo keyword, KHÔNG lọc trạng thái (dùng khi người dùng để "Tất cả trạng thái")
    // ⚠️ TRƯỚC ĐÂY khi không chọn trạng thái, code gọi findAllWithDeleted() bỏ qua hoàn toàn keyword,
    // nên gõ gì vào ô tìm kiếm cũng không lọc được (kể cả tên không tồn tại vẫn hiện hết).
    @Query("SELECT e FROM Employee e WHERE " +
            "(:keyword IS NULL OR " +
            "LOWER(e.employeeFullname) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.employeePhone) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.employeePosition) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.employeeEmail) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.user.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.user.username) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    Page<Employee> searchEmployeesNoStatus(@Param("keyword") String keyword, Pageable pageable);

    // ✅ Tìm kiếm theo keyword KẾT HỢP với lọc theo user.status (ACTIVE/BANNED/DELETED)
    // ⚠️ TRƯỚC ĐÂY khi chọn 1 trạng thái, code gọi findByUserStatus() bỏ qua hoàn toàn keyword.
    @Query("SELECT e FROM Employee e WHERE e.user.status = :userStatus AND (" +
            ":keyword IS NULL OR " +
            "LOWER(e.employeeFullname) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.employeePhone) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.employeePosition) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.employeeEmail) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.user.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
            "LOWER(e.user.username) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    Page<Employee> searchEmployeesByUserStatus(@Param("keyword") String keyword,
                                               @Param("userStatus") UserStatus userStatus,
                                               Pageable pageable);

    // ✅ Lấy tất cả nhân viên (kể cả đã xóa)
    @Query("SELECT e FROM Employee e")
    Page<Employee> findAllWithDeleted(Pageable pageable);

    // ✅ Lấy nhân viên chưa bị xóa (user.status <> DELETED)
    @Query("SELECT e FROM Employee e WHERE e.user.status <> 'DELETED'")
    Page<Employee> findAllActive(Pageable pageable);

    // ✅ Lọc nhân viên theo status của User
    @Query("SELECT e FROM Employee e WHERE e.user.status = :userStatus")
    Page<Employee> findByUserStatus(@Param("userStatus") UserStatus userStatus, Pageable pageable);

    // ✅ Đếm nhân viên theo status của User
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.user.status = :userStatus")
    long countByUserStatus(@Param("userStatus") UserStatus userStatus);

    // ✅ Tổng số nhân viên
    @Query("SELECT COUNT(e) FROM Employee e")
    long countTotalEmployees();

    // ✅ Đếm nhân viên đang hoạt động - dựa theo user.status (được cập nhật khi khóa/xóa/khôi phục)
    // ⚠️ TRƯỚC ĐÂY dùng e.employeeStatus = 'ACTIVE', nhưng field này KHÔNG bao giờ được
    // cập nhật khi khóa/mở khóa/xóa/khôi phục (những thao tác đó chỉ đổi user.status),
    // nên số "Đang hoạt động" luôn đứng yên dù Tạm khóa/Đã xóa đã thay đổi.
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.user.status = 'ACTIVE'")
    long countActiveEmployees();

    // Các method cũ giữ nguyên
    Optional<Employee> findByEmployeeId(Integer employeeId);

    boolean existsByEmployeePhone(String employeePhone);
    boolean existsByEmployeeEmail(String employeeEmail);

    @Query("SELECT COUNT(e) > 0 FROM Employee e WHERE e.employeePhone = :phone AND e.employeeId != :id")
    boolean existsByEmployeePhoneAndEmployeeIdNot(@Param("phone") String phone, @Param("id") Integer id);

    @Query("SELECT COUNT(e) > 0 FROM Employee e WHERE e.employeeEmail = :email AND e.employeeId != :id")
    boolean existsByEmployeeEmailAndEmployeeIdNot(@Param("email") String email, @Param("id") Integer id);

    // quanLyNhanVien/repo/EmployeeRepository.java
// THÊM method này vào cuối interface

    // ✅ Lấy Employee theo userId - SỬA employeeUser.userId THÀNH user.userId
    @Query("SELECT e FROM Employee e WHERE e.user.userId = :userId")
    Optional<Employee> findByEmployeeUserId(@Param("userId") Integer userId);

    // ✅ Kiểm tra tồn tại Employee theo userId - SỬA employeeUser.userId THÀNH user.userId
    @Query("SELECT COUNT(e) > 0 FROM Employee e WHERE e.user.userId = :userId")
    boolean existsByEmployeeUserId(@Param("userId") Integer userId);

    // ✅ Lấy danh sách staff rảnh rỗi (load balancing)
    @Query("SELECT e FROM Employee e " +
           "WHERE e.user.status = 'ACTIVE' " +
           "AND (e.currentChatCount IS NULL OR e.currentChatCount < e.maxChatCount) " +
           "ORDER BY e.currentChatCount ASC, e.lastActiveAt ASC")
    Page<Employee> findAvailableStaffForChat(Pageable pageable);
}