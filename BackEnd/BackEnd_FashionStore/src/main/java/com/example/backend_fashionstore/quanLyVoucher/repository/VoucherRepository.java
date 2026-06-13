package com.example.backend_fashionstore.quanLyVoucher.repository;

import com.example.backend_fashionstore.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

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

    // Thống kê tổng số (chưa xóa)
    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.voucherDeletedAt IS NULL")
    long countAllActive();

    // Thống kê số đang hoạt động (chưa xóa)
    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.voucherStatus = 'ACTIVE' AND v.voucherDeletedAt IS NULL")
    long countByStatusActive();

    // Thống kê số đã dừng (chưa xóa)
    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.voucherStatus = 'INACTIVE' AND v.voucherDeletedAt IS NULL")
    long countByStatusInactive();

    // Tổng số lần đã sử dụng (tất cả voucher chưa xóa)
    @Query("SELECT COALESCE(SUM(v.voucherUsedCount), 0) FROM Voucher v WHERE v.voucherDeletedAt IS NULL")
    long sumUsedCount();

    // Kiểm tra mã trùng (chưa xóa)
    @Query("SELECT COUNT(v) > 0 FROM Voucher v WHERE " +
            "LOWER(v.voucherCode) = LOWER(:code) AND v.voucherDeletedAt IS NULL")
    boolean existsByVoucherCode(@Param("code") String code);

    // Kiểm tra mã trùng khi sửa (chưa xóa)
    @Query("SELECT COUNT(v) > 0 FROM Voucher v WHERE " +
            "LOWER(v.voucherCode) = LOWER(:code) AND " +
            "v.voucherId != :id AND v.voucherDeletedAt IS NULL")
    boolean existsByVoucherCodeAndIdNot(@Param("code") String code, @Param("id") Integer id);
}