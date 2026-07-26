package com.example.backend_fashionstore.customer.wishlist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponseDTO {
    private Long wishlistId;
    private Integer customerId;
    private Integer totalItems;
    private List<WishlistItemResponseDTO> items;
}