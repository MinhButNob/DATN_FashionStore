package com.example.backend_fashionstore.customer.cart.dto.response;

import com.example.backend_fashionstore.customer.cart.dto.response.CartItemResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDTO {
    private Integer cartId;
    private Integer customerId;
    private List<CartItemResponseDTO> items;
    private BigDecimal subtotal;
    private Integer totalQuantity;
}