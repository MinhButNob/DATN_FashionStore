package com.example.backend_fashionstore.quanLyVoucher.controller;

import com.example.backend_fashionstore.quanLyVoucher.dto.VoucherDTO;
import com.example.backend_fashionstore.quanLyVoucher.service.CustomerVoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CustomerVoucherController {

    private final CustomerVoucherService customerVoucherService;

    /**
     * Lấy danh sách voucher có thể áp dụng cho khách hàng
     */
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableVouchers() {
        log.info("📋 Getting available vouchers for customer");

        try {
            List<VoucherDTO> vouchers = customerVoucherService.getAvailableVouchers();
            return ResponseEntity.ok(vouchers);
        } catch (Exception e) {
            log.error("❌ Error getting available vouchers: {}", e.getMessage());
            return ResponseEntity.status(500).body("Không thể tải danh sách mã giảm giá");
        }
    }

    /**
     * Lấy danh sách voucher có thể áp dụng với giá trị đơn hàng
     */
    @GetMapping("/available/by-order-value")
    public ResponseEntity<?> getAvailableVouchersByOrderValue(
            @RequestParam(defaultValue = "0") Long orderValue
    ) {
        log.info("📋 Getting available vouchers for order value: {}", orderValue);

        try {
            List<VoucherDTO> vouchers = customerVoucherService.getAvailableVouchersByOrderValue(
                    java.math.BigDecimal.valueOf(orderValue)
            );
            return ResponseEntity.ok(vouchers);
        } catch (Exception e) {
            log.error("❌ Error getting available vouchers: {}", e.getMessage());
            return ResponseEntity.status(500).body("Không thể tải danh sách mã giảm giá");
        }
    }

    /**
     * Kiểm tra voucher có áp dụng được không
     */
    @GetMapping("/check/{voucherCode}")
    public ResponseEntity<?> checkVoucher(
            @PathVariable String voucherCode,
            @RequestParam(defaultValue = "0") Long orderValue
    ) {
        log.info("🔍 Checking voucher: {} for order value: {}", voucherCode, orderValue);

        try {
            boolean isValid = customerVoucherService.isVoucherValid(
                    voucherCode,
                    java.math.BigDecimal.valueOf(orderValue)
            );
            return ResponseEntity.ok(isValid);
        } catch (Exception e) {
            log.error("❌ Error checking voucher: {}", e.getMessage());
            return ResponseEntity.status(500).body("Không thể kiểm tra mã giảm giá");
        }
    }
}