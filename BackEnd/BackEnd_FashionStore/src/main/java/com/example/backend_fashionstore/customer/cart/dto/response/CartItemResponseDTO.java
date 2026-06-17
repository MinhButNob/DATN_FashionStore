package com.example.backend_fashionstore.customer.cart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDTO {
    private Integer cartItemId;
    private Integer productDetailId;
    private Integer productId;
    private String productName;
    private String colorName;
    private String sizeName;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
    private String imageUrl;
    private Integer stock;  // ✅ THÊM DÒNG NÀY - Lấy từ ProductDetail.quantity
}