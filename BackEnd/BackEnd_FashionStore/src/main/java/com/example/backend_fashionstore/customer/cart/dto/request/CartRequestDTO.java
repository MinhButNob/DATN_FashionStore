package com.example.backend_fashionstore.customer.cart.dto.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartRequestDTO {
    private Integer productDetailId;
    private Integer quantity;

    /**
     * Get quantity with default value = 1 if null or invalid
     */
    public Integer getQuantity() {
        if (quantity == null || quantity <= 0) {
            return 1;
        }
        return quantity;
    }

    /**
     * Set quantity with validation
     */
    public void setQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            this.quantity = 1;
        } else {
            this.quantity = quantity;
        }
    }
}
