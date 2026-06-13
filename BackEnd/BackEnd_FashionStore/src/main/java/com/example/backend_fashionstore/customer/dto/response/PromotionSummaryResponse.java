// PromotionSummaryResponse.java
package com.example.backend_fashionstore.customer.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PromotionSummaryResponse {
    private Integer promotionId;
    private String promotionName;
    private BigDecimal promotionDiscountPercent;
}