package com.example.backend_fashionstore.shipping.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingCalculateResponse {
    private BigDecimal shippingFee;
    private Integer weight;
}
