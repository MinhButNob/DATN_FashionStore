package com.example.backend_fashionstore.quanLyVoucher.repository;

import com.example.backend_fashionstore.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    // ===== ADMIN QUERIES =====

    // Lấy tất cả (chưa xóa)
    @Query("SELECT v FROM Voucher v WHERE v.voucherDeletedAt IS NULL")
    Page<Voucher> findAllActive(Pageable pageable);

    // Lấy danh sách đã xóa
    @Query("SELECT v FROM Voucher v WHERE v.voucherDeletedAt IS NOT NULL")
    Page<Voucher> findDeletedVouchers(Pageable pageable);

    // Tìm kiếm theo mã (chưa xóa)
    @Query("SELECT v FROM Voucher v WHERE " +
            "LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND v.voucherDeletedAt IS NULL")
    Page<Voucher> searchByVoucherCode(@Param("keyword") String keyword, Pageable pageable);

    // Lọc theo trạng thái (chưa xóa)
    @Query("SELECT v FROM Voucher v WHERE " +
            "v.voucherStatus = :status AND v.voucherDeletedAt IS NULL")
    Page<Voucher> findByVoucherStatus(@Param("status") String status, Pageable pageable);

    // Lọc + tìm kiếm (chưa xóa)
    @Query("SELECT v FROM Voucher v WHERE " +
            "v.voucherStatus = :status AND " +
            "LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " +
            "v.voucherDeletedAt IS NULL")
    Page<Voucher> searchByVoucherStatusAndCode(
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable);

    // ===== STATISTICS =====

    // Thống kê tổng số (chưa xóa)
    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.voucherDeletedAt IS NULL")
    long countAllActive();

    // Thống kê số đang hoạt động (chưa xóa)
    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.voucherStatus = 'ACTIVE' AND v.voucherDeletedAt IS NULL")
    long countByStatusActive();

    // Thống kê số đã dừng/hết hạn (chưa xóa)
    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.voucherStatus IN ('DISABLED', 'EXPIRED') AND v.voucherDeletedAt IS NULL")
    long countByStatusInactive();

    // Tổng số lần đã sử dụng (tất cả voucher chưa xóa)
    @Query("SELECT COALESCE(SUM(v.voucherUsedCount), 0) FROM Voucher v WHERE v.voucherDeletedAt IS NULL")
    long sumUsedCount();

    // ===== VALIDATION =====

    // Kiểm tra mã trùng (chưa xóa)
    @Query("SELECT COUNT(v) > 0 FROM Voucher v WHERE " +
            "LOWER(v.voucherCode) = LOWER(:code) AND v.voucherDeletedAt IS NULL")
    boolean existsByVoucherCode(@Param("code") String code);

    // Kiểm tra mã trùng khi sửa (chưa xóa)
    @Query("SELECT COUNT(v) > 0 FROM Voucher v WHERE " +
            "LOWER(v.voucherCode) = LOWER(:code) AND " +
            "v.voucherId != :id AND v.voucherDeletedAt IS NULL")
    boolean existsByVoucherCodeAndIdNot(@Param("code") String code, @Param("id") Integer id);

    // ===== CUSTOMER QUERIES (BỔ SUNG) =====

    // Tìm voucher theo code (chưa xóa) - đã có
    @Query("SELECT v FROM Voucher v WHERE v.voucherCode = :code AND v.voucherDeletedAt IS NULL")
    Optional<Voucher> findByVoucherCode(@Param("code") String code);

    // ✅ Tìm voucher theo code và status ACTIVE (chưa xóa)
    @Query("SELECT v FROM Voucher v WHERE v.voucherCode = :code AND v.voucherStatus = 'ACTIVE' AND v.voucherDeletedAt IS NULL")
    Optional<Voucher> findActiveByVoucherCode(@Param("code") String code);

    // ✅ Lấy danh sách voucher đang hoạt động, còn hạn, chưa dùng hết
    @Query("SELECT v FROM Voucher v WHERE " +
            "v.voucherStatus = 'ACTIVE' " +
            "AND v.voucherDeletedAt IS NULL " +
            "AND v.voucherStartDate <= :now " +
            "AND v.voucherEndDate >= :now " +
            "AND (v.voucherMaxUsage IS NULL OR v.voucherUsedCount < v.voucherMaxUsage)")
    List<Voucher> findAvailableVouchers(@Param("now") LocalDateTime now);

    // ✅ Lấy danh sách voucher có thể áp dụng với giá trị đơn hàng
    @Query("SELECT v FROM Voucher v WHERE " +
            "v.voucherStatus = 'ACTIVE' " +
            "AND v.voucherDeletedAt IS NULL " +
            "AND v.voucherStartDate <= :now " +
            "AND v.voucherEndDate >= :now " +
            "AND (v.voucherMaxUsage IS NULL OR v.voucherUsedCount < v.voucherMaxUsage) " +
            "AND (v.voucherMinOrderValue IS NULL OR v.voucherMinOrderValue <= :orderValue)")
    List<Voucher> findAvailableVouchersByOrderValue(
            @Param("orderValue") BigDecimal orderValue,
            @Param("now") LocalDateTime now);

    // ✅ Đếm số voucher đang hoạt động (customer)
    @Query("SELECT COUNT(v) FROM Voucher v WHERE " +
            "v.voucherStatus = 'ACTIVE' " +
            "AND v.voucherDeletedAt IS NULL " +
            "AND v.voucherStartDate <= :now " +
            "AND v.voucherEndDate >= :now " +
            "AND (v.voucherMaxUsage IS NULL OR v.voucherUsedCount < v.voucherMaxUsage)")
    long countAvailableVouchers(@Param("now") LocalDateTime now);

    // ✅ Kiểm tra voucher có tồn tại và active không
    @Query("SELECT COUNT(v) > 0 FROM Voucher v WHERE " +
            "v.voucherCode = :code " +
            "AND v.voucherStatus = 'ACTIVE' " +
            "AND v.voucherDeletedAt IS NULL " +
            "AND v.voucherStartDate <= :now " +
            "AND v.voucherEndDate >= :now " +
            "AND (v.voucherMaxUsage IS NULL OR v.voucherUsedCount < v.voucherMaxUsage)")
    boolean existsActiveVoucherByCode(@Param("code") String code, @Param("now") LocalDateTime now);

    // ✅ Lấy voucher sắp hết hạn (còn 7 ngày)
    @Query("SELECT v FROM Voucher v WHERE " +
            "v.voucherStatus = 'ACTIVE' " +
            "AND v.voucherDeletedAt IS NULL " +
            "AND v.voucherEndDate BETWEEN :now AND :expiryDate " +
            "AND (v.voucherMaxUsage IS NULL OR v.voucherUsedCount < v.voucherMaxUsage)")
    List<Voucher> findExpiringSoonVouchers(
            @Param("now") LocalDateTime now,
            @Param("expiryDate") LocalDateTime expiryDate);

    // ✅ Lấy voucher mới nhất
    @Query("SELECT v FROM Voucher v WHERE " +
            "v.voucherStatus = 'ACTIVE' " +
            "AND v.voucherDeletedAt IS NULL " +
            "AND v.voucherStartDate <= :now " +
            "AND v.voucherEndDate >= :now " +
            "ORDER BY v.voucherCreatedAt DESC")
    List<Voucher> findLatestVouchers(@Param("now") LocalDateTime now, Pageable pageable);

    // ✅ Lấy voucher theo status và hạn sử dụng
    @Query("SELECT v FROM Voucher v WHERE v.voucherStatus = :status AND v.voucherEndDate >= :now AND v.voucherDeletedAt IS NULL")
    List<Voucher> findByStatusAndExpiryDateAfter(@Param("status") String status, @Param("now") LocalDateTime now);

    // ✅ Lấy voucher customer chưa sử dụng
    @Query("SELECT v FROM Voucher v WHERE v.voucherStatus = 'ACTIVE' AND v.voucherDeletedAt IS NULL AND v.voucherEndDate >= CURRENT_TIMESTAMP AND v.voucherId NOT IN (SELECT vu.voucher.voucherId FROM VoucherUsage vu WHERE vu.customer.customerId = :customerId)")
    List<Voucher> findUnusedVouchersByCustomer(@Param("customerId") Integer customerId);

    // ✅ Tự động hủy/dừng các voucher đã hết hạn
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Voucher v SET v.voucherStatus = 'EXPIRED' WHERE v.voucherStatus = 'ACTIVE' AND v.voucherEndDate < :now AND v.voucherDeletedAt IS NULL")
    int deactivateExpiredVouchers(@Param("now") LocalDateTime now);

    // ✅ Lọc theo tập hợp các trạng thái
    @Query("SELECT v FROM Voucher v WHERE v.voucherStatus IN :statuses AND v.voucherDeletedAt IS NULL")
    Page<Voucher> findByVoucherStatusIn(@Param("statuses") List<String> statuses, Pageable pageable);

    // ✅ Lọc + tìm kiếm theo tập hợp các trạng thái
    @Query("SELECT v FROM Voucher v WHERE v.voucherStatus IN :statuses AND LOWER(v.voucherCode) LIKE LOWER(CONCAT('%', :keyword, '%')) AND v.voucherDeletedAt IS NULL")
    Page<Voucher> searchByVoucherStatusInAndCode(@Param("statuses") List<String> statuses, @Param("keyword") String keyword, Pageable pageable);
}