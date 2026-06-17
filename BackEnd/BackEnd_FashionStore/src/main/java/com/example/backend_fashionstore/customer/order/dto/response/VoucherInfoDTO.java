package com.example.backend_fashionstore.customer.order.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherInfoDTO {
    private Integer voucherId;
    private String voucherCode;
    private BigDecimal discountAmount;
    private BigDecimal discountPercent;
}