package com.example.backend_fashionstore.quanLyVoucher.service;

import com.example.backend_fashionstore.entity.Voucher;
import com.example.backend_fashionstore.quanLyVoucher.dto.*;
import com.example.backend_fashionstore.quanLyVoucher.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;

    // Lấy danh sách (chỉ hiển thị chưa xóa)
    @Transactional
    public Page<VoucherDTO> getAllVouchers(Pageable pageable) {
        autoDeactivateExpiredVouchers();
        return voucherRepository.findAllActive(pageable)
                .map(this::convertToDTO);
    }

    // Lấy danh sách đã xóa
    @Transactional
    public Page<VoucherDTO> getDeletedVouchers(Pageable pageable) {
        autoDeactivateExpiredVouchers();
        return voucherRepository.findDeletedVouchers(pageable)
                .map(this::convertToDTO);
    }

    // Tìm kiếm theo mã
    @Transactional
    public Page<VoucherDTO> searchVouchers(String keyword, Pageable pageable) {
        autoDeactivateExpiredVouchers();
        return voucherRepository.searchByVoucherCode(keyword, pageable)
                .map(this::convertToDTO);
    }

    // Lọc theo trạng thái
    @Transactional
    public Page<VoucherDTO> filterVouchersByStatus(String status, Pageable pageable) {
        autoDeactivateExpiredVouchers();
        if ("INACTIVE".equals(status)) {
            return voucherRepository.findByVoucherStatusIn(java.util.List.of("DISABLED", "EXPIRED"), pageable)
                    .map(this::convertToDTO);
        }
        return voucherRepository.findByVoucherStatus(status, pageable)
                .map(this::convertToDTO);
    }

    // Lọc + tìm kiếm
    @Transactional
    public Page<VoucherDTO> filterAndSearchVouchers(String status, String keyword, Pageable pageable) {
        autoDeactivateExpiredVouchers();
        if (keyword != null && !keyword.isEmpty() && status != null && !status.isEmpty()) {
            if ("INACTIVE".equals(status)) {
                return voucherRepository.searchByVoucherStatusInAndCode(java.util.List.of("DISABLED", "EXPIRED"), keyword, pageable)
                        .map(this::convertToDTO);
            }
            return voucherRepository.searchByVoucherStatusAndCode(status, keyword, pageable)
                    .map(this::convertToDTO);
        } else if (keyword != null && !keyword.isEmpty()) {
            return searchVouchers(keyword, pageable);
        } else if (status != null && !status.isEmpty()) {
            return filterVouchersByStatus(status, pageable);
        } else {
            return getAllVouchers(pageable);
        }
    }

    public VoucherDTO getVoucherById(Integer id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));
        return convertToDTO(voucher);
    }

    @Transactional
    public VoucherDTO createVoucher(VoucherCreateDTO dto) {
        // Kiểm tra mã trùng
        if (voucherRepository.existsByVoucherCode(dto.getVoucherCode())) {
            throw new RuntimeException("Mã voucher đã tồn tại: " + dto.getVoucherCode());
        }

        // Kiểm tra ngày hợp lệ
        if (dto.getVoucherEndDate().isBefore(dto.getVoucherStartDate())) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        // Kiểm tra chỉ được chọn 1 loại giảm giá
        if ((dto.getVoucherDiscountAmount() != null && dto.getVoucherDiscountAmount().compareTo(BigDecimal.ZERO) > 0) &&
                (dto.getVoucherDiscountPercent() != null && dto.getVoucherDiscountPercent().compareTo(BigDecimal.ZERO) > 0)) {
            throw new RuntimeException("Chỉ được chọn một loại giảm giá: theo số tiền hoặc theo phần trăm");
        }

        if ((dto.getVoucherDiscountAmount() == null || dto.getVoucherDiscountAmount().compareTo(BigDecimal.ZERO) == 0) &&
                (dto.getVoucherDiscountPercent() == null || dto.getVoucherDiscountPercent().compareTo(BigDecimal.ZERO) == 0)) {
            throw new RuntimeException("Phải chọn một loại giảm giá");
        }

        Voucher voucher = Voucher.builder()
                .voucherCode(dto.getVoucherCode())
                .voucherDiscountAmount(dto.getVoucherDiscountAmount())
                .voucherDiscountPercent(dto.getVoucherDiscountPercent())
                .voucherStartDate(dto.getVoucherStartDate())
                .voucherEndDate(dto.getVoucherEndDate())
                .voucherMaxUsage(dto.getVoucherMaxUsage())
                .voucherUsedCount(0)
                .voucherMinOrderValue(dto.getVoucherMinOrderValue() != null ? dto.getVoucherMinOrderValue() : BigDecimal.ZERO)
                .voucherStatus("ACTIVE")
                .build();
        voucher = voucherRepository.save(voucher);
        return convertToDTO(voucher);
    }

    @Transactional
    public VoucherDTO updateVoucher(Integer id, VoucherUpdateDTO dto) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));

        if (voucher.getVoucherDeletedAt() != null) {
            throw new RuntimeException("Voucher đã bị xóa, không thể cập nhật");
        }

        // Kiểm tra mã trùng
        if (voucherRepository.existsByVoucherCodeAndIdNot(dto.getVoucherCode(), id)) {
            throw new RuntimeException("Mã voucher đã tồn tại: " + dto.getVoucherCode());
        }

        // Kiểm tra ngày hợp lệ
        if (dto.getVoucherEndDate().isBefore(dto.getVoucherStartDate())) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        voucher.setVoucherCode(dto.getVoucherCode());
        voucher.setVoucherDiscountAmount(dto.getVoucherDiscountAmount());
        voucher.setVoucherDiscountPercent(dto.getVoucherDiscountPercent());
        voucher.setVoucherStartDate(dto.getVoucherStartDate());
        voucher.setVoucherEndDate(dto.getVoucherEndDate());
        voucher.setVoucherMaxUsage(dto.getVoucherMaxUsage());
        voucher.setVoucherMinOrderValue(dto.getVoucherMinOrderValue() != null ? dto.getVoucherMinOrderValue() : BigDecimal.ZERO);
        voucher.setVoucherUpdatedAt(LocalDateTime.now());
        voucher = voucherRepository.save(voucher);
        return convertToDTO(voucher);
    }

    // Xóa mềm (ẩn)
    @Transactional
    public void softDeleteVoucher(Integer id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));

        if (voucher.getVoucherDeletedAt() != null) {
            throw new RuntimeException("Voucher đã bị xóa trước đó");
        }

        voucher.setVoucherDeletedAt(LocalDateTime.now());
        voucher.setVoucherUpdatedAt(LocalDateTime.now());
        voucherRepository.save(voucher);
    }

    // Khôi phục
    @Transactional
    public void restoreVoucher(Integer id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));

        if (voucher.getVoucherDeletedAt() == null) {
            throw new RuntimeException("Voucher chưa bị xóa, không cần khôi phục");
        }

        voucher.setVoucherDeletedAt(null);
        voucher.setVoucherUpdatedAt(LocalDateTime.now());
        voucherRepository.save(voucher);
    }

    @Transactional
    public void toggleVoucherStatus(Integer id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));

        if (voucher.getVoucherDeletedAt() != null) {
            throw new RuntimeException("Voucher đã bị xóa, không thể thay đổi trạng thái");
        }

        if ("ACTIVE".equals(voucher.getVoucherStatus())) {
            voucher.setVoucherStatus("DISABLED");
        } else {
            if (voucher.getVoucherEndDate().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Voucher đã hết hạn, không thể kích hoạt. Vui lòng cập nhật lại ngày kết thúc trước.");
            }
            voucher.setVoucherStatus("ACTIVE");
        }
        voucher.setVoucherUpdatedAt(LocalDateTime.now());
        voucherRepository.save(voucher);
    }

    @Transactional
    public VoucherStatisticsDTO getStatistics() {
        autoDeactivateExpiredVouchers();
        long total = voucherRepository.countAllActive();
        long active = voucherRepository.countByStatusActive();
        long inactive = voucherRepository.countByStatusInactive();
        long totalUsedCount = voucherRepository.sumUsedCount();

        return new VoucherStatisticsDTO(total, active, inactive, totalUsedCount);
    }

    private VoucherDTO convertToDTO(Voucher v) {
        VoucherDTO dto = new VoucherDTO();
        dto.setVoucherId(v.getVoucherId());
        dto.setVoucherCode(v.getVoucherCode());
        dto.setVoucherDiscountAmount(v.getVoucherDiscountAmount());
        dto.setVoucherDiscountPercent(v.getVoucherDiscountPercent());
        dto.setVoucherStartDate(v.getVoucherStartDate());
        dto.setVoucherEndDate(v.getVoucherEndDate());
        dto.setVoucherMaxUsage(v.getVoucherMaxUsage());
        dto.setVoucherUsedCount(v.getVoucherUsedCount());
        dto.setVoucherMinOrderValue(v.getVoucherMinOrderValue());
        dto.setVoucherStatus(v.getVoucherStatus());
        dto.setVoucherCreatedAt(v.getVoucherCreatedAt());
        dto.setVoucherUpdatedAt(v.getVoucherUpdatedAt());
        dto.setVoucherDeletedAt(v.getVoucherDeletedAt());
        return dto;
    }

    @Transactional
    public void autoDeactivateExpiredVouchers() {
        voucherRepository.deactivateExpiredVouchers(LocalDateTime.now());
    }
}