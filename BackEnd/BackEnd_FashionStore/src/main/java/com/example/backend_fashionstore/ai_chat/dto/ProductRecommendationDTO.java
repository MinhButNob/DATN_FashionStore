package com.example.backend_fashionstore.ai_chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRecommendationDTO {
    private Integer id;
    private String name;
    private Double price;
    private String category;
    private String color;
    private String size;
    private String link; // URL to the product image
}
