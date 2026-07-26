package com.example.backend_fashionstore.quanLyVoucher.controller;

import com.example.backend_fashionstore.QuanLySanPham.dto.response.ApiResponse;
import com.example.backend_fashionstore.quanLyVoucher.dto.VoucherDTO;
import com.example.backend_fashionstore.quanLyVoucher.service.CustomerVoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class CustomerVoucherController {

    private final CustomerVoucherService customerVoucherService;

    /**
     * Lấy danh sách voucher có thể áp dụng cho khách hàng
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<VoucherDTO>>> getAvailableVouchers() {
        log.info("📋 Getting available vouchers for customer");

        try {
            List<VoucherDTO> vouchers = customerVoucherService.getAvailableVouchers();
            log.info("✅ Found {} available vouchers", vouchers.size());
            return ResponseEntity.ok(ApiResponse.success(vouchers));
        } catch (Exception e) {
            log.error("❌ Error getting available vouchers: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("Không thể tải danh sách mã giảm giá"));
        }
    }

    /**
     * Lấy danh sách voucher có thể áp dụng với giá trị đơn hàng
     */
    @GetMapping("/available/by-order-value")
    public ResponseEntity<ApiResponse<List<VoucherDTO>>> getAvailableVouchersByOrderValue(
            @RequestParam(defaultValue = "0") Long orderValue
    ) {
        log.info("📋 Getting available vouchers for order value: {}", orderValue);

        try {
            List<VoucherDTO> vouchers = customerVoucherService.getAvailableVouchersByOrderValue(
                    BigDecimal.valueOf(orderValue)
            );
            return ResponseEntity.ok(ApiResponse.success(vouchers));
        } catch (Exception e) {
            log.error("❌ Error getting available vouchers: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("Không thể tải danh sách mã giảm giá"));
        }
    }

    /**
     * Kiểm tra voucher có áp dụng được không
     */
    @GetMapping("/check/{voucherCode}")
    public ResponseEntity<ApiResponse<Boolean>> checkVoucher(
            @PathVariable String voucherCode,
            @RequestParam(defaultValue = "0") Long orderValue
    ) {
        log.info("🔍 Checking voucher: {} for order value: {}", voucherCode, orderValue);

        try {
            boolean isValid = customerVoucherService.isVoucherValid(
                    voucherCode,
                    BigDecimal.valueOf(orderValue)
            );
            return ResponseEntity.ok(ApiResponse.success(isValid));
        } catch (Exception e) {
            log.error("❌ Error checking voucher: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("Không thể kiểm tra mã giảm giá"));
        }
    }
}