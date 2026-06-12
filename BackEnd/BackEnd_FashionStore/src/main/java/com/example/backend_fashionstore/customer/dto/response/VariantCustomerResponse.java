// VariantCustomerResponse.java
package com.example.backend_fashionstore.customer.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class VariantCustomerResponse {
    private Integer productDetailId;
    private String sku;
    private String colorName;
    private String colorCode;
    private String hexCode;
    private String sizeName;
    private String sizeCode;
    private BigDecimal price;
    private Integer quantity;
    private boolean inStock;
}