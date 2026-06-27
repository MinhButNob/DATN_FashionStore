package com.example.backend_fashionstore.ai_chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponseDTO {
    private String response;
    private List<ProductRecommendationDTO> products;
    private String intent;
    // ID sản phẩm Haru muốn điều hướng tới (khi intent = OPEN_PRODUCT)
    private Integer navigateToProductId;
}