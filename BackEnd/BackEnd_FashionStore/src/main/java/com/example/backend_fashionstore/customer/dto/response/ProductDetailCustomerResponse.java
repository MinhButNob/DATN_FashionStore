// ProductDetailCustomerResponse.java
package com.example.backend_fashionstore.customer.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductDetailCustomerResponse {
    private Integer productId;
    private String productName;
    private String description;
    private String categoryName;
    private Integer categoryId;
    private List<VariantCustomerResponse> variants;
    private List<ImageCustomerResponse> images;
    private List<ColorOptionResponse> colorOptions;
    private List<SizeOptionResponse> sizeOptions;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer totalStock;

    // ✅ Thêm promotion cho sản phẩm
    private PromotionSummaryResponse promotion;
}