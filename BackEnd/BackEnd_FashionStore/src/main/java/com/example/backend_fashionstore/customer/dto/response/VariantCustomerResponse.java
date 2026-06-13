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

    // ✅ Thêm colorId và sizeId để frontend dễ xử lý
    private Integer colorId;
    private String colorName;
    private String colorCode;
    private String hexCode;

    private Integer sizeId;
    private String sizeName;
    private String sizeCode;

    private BigDecimal price;
    private BigDecimal discountedPrice;  // ✅ Thêm giá sau khuyến mãi
    private Integer quantity;
    private boolean inStock;
}