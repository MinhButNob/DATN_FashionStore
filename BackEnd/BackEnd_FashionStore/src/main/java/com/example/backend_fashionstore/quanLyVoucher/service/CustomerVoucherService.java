package com.example.backend_fashionstore.quanLyVoucher.service;

import com.example.backend_fashionstore.entity.Voucher;
import com.example.backend_fashionstore.quanLyVoucher.dto.VoucherDTO;
import com.example.backend_fashionstore.quanLyVoucher.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerVoucherService {

    private final VoucherRepository voucherRepository;

    /**
     * Lấy danh sách voucher có thể áp dụng (đang hoạt động, còn hạn, chưa dùng hết)
     */
    public List<VoucherDTO> getAvailableVouchers() {
        LocalDateTime now = LocalDateTime.now();

        List<Voucher> vouchers = voucherRepository.findAvailableVouchers(now);

        log.info("✅ Found {} available vouchers", vouchers.size());

        return vouchers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách voucher có thể áp dụng với giá trị đơn hàng
     */
    public List<VoucherDTO> getAvailableVouchersByOrderValue(BigDecimal orderValue) {
        LocalDateTime now = LocalDateTime.now();

        List<Voucher> vouchers = voucherRepository.findAvailableVouchersByOrderValue(orderValue, now);

        log.info("✅ Found {} available vouchers for order value: {}", vouchers.size(), orderValue);

        return vouchers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra voucher có hợp lệ không
     */
    public boolean isVoucherValid(String voucherCode, BigDecimal orderValue) {
        if (voucherCode == null || voucherCode.isEmpty()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        Voucher voucher = voucherRepository.findActiveByVoucherCode(voucherCode)
                .orElse(null);

        if (voucher == null) {
            log.warn("❌ Voucher not found: {}", voucherCode);
            return false;
        }

        // Kiểm tra trạng thái
        if (!"ACTIVE".equals(voucher.getVoucherStatus()) || voucher.getVoucherDeletedAt() != null) {
            log.warn("❌ Voucher not active: {}", voucherCode);
            return false;
        }

        // Kiểm tra thời gian
        if (now.isBefore(voucher.getVoucherStartDate()) || now.isAfter(voucher.getVoucherEndDate())) {
            log.warn("❌ Voucher expired: {}", voucherCode);
            return false;
        }

        // Kiểm tra số lần sử dụng
        if (voucher.getVoucherMaxUsage() != null && voucher.getVoucherUsedCount() >= voucher.getVoucherMaxUsage()) {
            log.warn("❌ Voucher used up: {}", voucherCode);
            return false;
        }

        // Kiểm tra giá trị đơn hàng tối thiểu
        if (voucher.getVoucherMinOrderValue() != null &&
                orderValue.compareTo(voucher.getVoucherMinOrderValue()) < 0) {
            log.warn("❌ Order value {} is less than min order value {}",
                    orderValue, voucher.getVoucherMinOrderValue());
            return false;
        }

        log.info("✅ Voucher valid: {}", voucherCode);
        return true;
    }

    /**
     * Lấy voucher theo mã
     */
    public VoucherDTO getVoucherByCode(String voucherCode) {
        Voucher voucher = voucherRepository.findActiveByVoucherCode(voucherCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã giảm giá: " + voucherCode));

        return convertToDTO(voucher);
    }

    /**
     * Chuyển đổi Voucher entity sang VoucherDTO
     */
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
}