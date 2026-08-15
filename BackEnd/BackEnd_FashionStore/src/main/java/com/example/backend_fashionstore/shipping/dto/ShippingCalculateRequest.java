package com.example.backend_fashionstore.shipping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingCalculateRequest {
    @NotBlank private String province;
    @NotBlank private String district;
    @NotBlank private String ward;
    private String address;
    @NotNull private BigDecimal orderValue;
    /** Chỉ dùng làm giá dự phòng khi frontend đã có quote nhưng GHTK tạm thời lỗi lúc tạo đơn. */
    private BigDecimal shippingFee;
}
