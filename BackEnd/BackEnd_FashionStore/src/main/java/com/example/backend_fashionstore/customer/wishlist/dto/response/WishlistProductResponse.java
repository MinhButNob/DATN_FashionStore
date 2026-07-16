package com.example.backend_fashionstore.customer.wishlist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistProductResponse {
    private Integer id;
    private String name;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private Double rating;
    private Integer sold;
    private String image;
    private Boolean inStock;
}
