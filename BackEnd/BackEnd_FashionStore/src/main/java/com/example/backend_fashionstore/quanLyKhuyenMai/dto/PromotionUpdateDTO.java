package com.example.backend_fashionstore.quanLyKhuyenMai.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionUpdateDTO {

    @NotBlank(message = "Tên khuyến mãi không được để trống")
    @Size(max = 20, message = "Tên khuyến mãi tối đa 20 ký tự")
    private String promotionName;

    @Size(max = 20, message = "Mô tả tối đa 20 ký tự")
    private String promotionDescription;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime promotionStartDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime promotionEndDate;

    @DecimalMin(value = "0.0", message = "Phần trăm giảm tối thiểu 0%")
    @DecimalMax(value = "100.0", message = "Phần trăm giảm tối đa 100%")
    private BigDecimal promotionDiscountPercent;
}