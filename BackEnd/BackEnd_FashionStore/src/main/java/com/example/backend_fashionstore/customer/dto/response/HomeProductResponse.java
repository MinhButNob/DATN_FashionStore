// HomeProductResponse.java
package com.example.backend_fashionstore.customer.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class HomeProductResponse {
    private List<ProductCardResponse> products;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}