package com.example.backend_fashionstore.ai_chat.service;

import com.example.backend_fashionstore.QuanLySanPham.repository.Product.ProductRepository;
import com.example.backend_fashionstore.ai_chat.dto.ProductRecommendationDTO;
import com.example.backend_fashionstore.entity.Product;
import com.example.backend_fashionstore.entity.ProductDetail;
import com.example.backend_fashionstore.entity.ProductImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductRecommendationService {

    private final ProductRepository productRepository;

    public List<ProductRecommendationDTO> recommendProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Product> products = productRepository.searchActiveProductsByKeyword(keyword.trim());
        
        // Take top 5 recommendations
        return products.stream()
                .limit(5)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private ProductRecommendationDTO mapToDTO(Product product) {
        ProductRecommendationDTO dto = new ProductRecommendationDTO();
        dto.setId(product.getProductId());
        dto.setName(product.getProductName());

        // Find the first active detail
        if (product.getProductDetails() != null && !product.getProductDetails().isEmpty()) {
            ProductDetail firstDetail = product.getProductDetails().get(0);
            
            // Set price, color, size
            if (firstDetail.getPrice() != null) {
                dto.setPrice(firstDetail.getPrice().doubleValue());
            }
            if (firstDetail.getColor() != null) {
                dto.setColor(firstDetail.getColor().getColorName());
            }
            if (firstDetail.getSize() != null) {
                dto.setSize(firstDetail.getSize().getSizeName());
            }
            
            // Set image link if available
            if (firstDetail.getProductImages() != null && !firstDetail.getProductImages().isEmpty()) {
                ProductImage firstImage = firstDetail.getProductImages().get(0);
                dto.setLink(firstImage.getProductImagePath());
            }
        }

        if (product.getCategory() != null) {
            dto.setCategory(product.getCategory().getCategoryName());
        }

        return dto;
    }
}
