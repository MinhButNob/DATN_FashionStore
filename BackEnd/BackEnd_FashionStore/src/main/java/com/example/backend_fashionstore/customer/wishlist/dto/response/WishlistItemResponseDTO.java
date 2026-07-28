package com.example.backend_fashionstore.customer.wishlist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemResponseDTO {
    private Integer wishlistItemId;
    private Integer productDetailId;
    private Integer productId;
    private String productName;
    private String colorName;
    private String sizeName;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private String imageUrl;
    private Integer stock;
    private LocalDateTime addedAt;
}