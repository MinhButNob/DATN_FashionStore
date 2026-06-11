package com.example.backend_fashionstore.quanLyVoucher.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherUpdateDTO {

    @NotBlank(message = "Mã voucher không được để trống")
    @Size(max = 20, message = "Mã voucher tối đa 20 ký tự")
    private String voucherCode;

    @DecimalMin(value = "0.0", message = "Số tiền giảm phải >= 0")
    private BigDecimal voucherDiscountAmount;

    @DecimalMin(value = "0.0", message = "Phần trăm giảm phải >= 0")
    @DecimalMax(value = "100.0", message = "Phần trăm giảm tối đa 100%")
    private BigDecimal voucherDiscountPercent;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime voucherStartDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime voucherEndDate;

    @Min(value = 1, message = "Số lần sử dụng tối đa phải >= 1")
    private Integer voucherMaxUsage;

    @DecimalMin(value = "0.0", message = "Giá trị đơn hàng tối thiểu phải >= 0")
    private BigDecimal voucherMinOrderValue;
}