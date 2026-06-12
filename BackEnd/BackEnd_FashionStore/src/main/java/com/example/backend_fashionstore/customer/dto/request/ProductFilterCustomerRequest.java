// ProductFilterCustomerRequest.java
package com.example.backend_fashionstore.customer.dto.request;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ProductFilterCustomerRequest {
    private String keyword;
    private Integer categoryId;
    private Integer colorId;
    private Integer sizeId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sortBy;
    @Builder.Default private Integer page = 0;
    @Builder.Default private Integer size = 12;
}