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
import java.util.Optional;
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

        // Filter for active and in-stock products
        products = products.stream()
                .filter(p -> p.getProductDetails().stream()
                        .anyMatch(pd -> "ACTIVE".equals(pd.getStatus()) && pd.getDeletedAt() == null && pd.getQuantity() != null && pd.getQuantity() > 0))
                .collect(Collectors.toList());

        // Take top 5 recommendations
        return products.stream()
                .limit(5)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<ProductRecommendationDTO> recommendByFilters(Integer categoryId, Integer colorId, Integer sizeId, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice) {
        // Here we could implement dynamic filtering by using a JPA Specification or similar.
        // For simplicity, we can fetch active products and filter in memory, or use multiple repository methods.
        // Given we added specific repository methods, we can combine them or just fetch all active and filter.
        List<Product> products = productRepository.findAllActiveProducts();

        return products.stream()
                .filter(p -> {
                    if (categoryId != null && (p.getCategory() == null || !categoryId.equals(p.getCategory().getCategoryId()))) return false;
                    boolean hasMatchingDetail = p.getProductDetails().stream().anyMatch(pd -> {
                        if (!"ACTIVE".equals(pd.getStatus()) || pd.getDeletedAt() != null || pd.getQuantity() == null || pd.getQuantity() <= 0) return false;
                        if (colorId != null && (pd.getColor() == null || !colorId.equals(pd.getColor().getColorId()))) return false;
                        if (sizeId != null && (pd.getSize() == null || !sizeId.equals(pd.getSize().getSizeId()))) return false;
                        if (minPrice != null && (pd.getPrice() == null || pd.getPrice().compareTo(minPrice) < 0)) return false;
                        if (maxPrice != null && (pd.getPrice() == null || pd.getPrice().compareTo(maxPrice) > 0)) return false;
                        return true;
                    });
                    return hasMatchingDetail;
                })
                .limit(5)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy 1 product card theo productId, dùng khi Haru muốn hiển thị
     * chi tiết một sản phẩm cụ thể đang được khách hỏi (intent OPEN_PRODUCT).
     */
    public Optional<ProductRecommendationDTO> getProductCardById(Integer productId) {
        if (productId == null) return Optional.empty();
        return productRepository.findById(productId)
                .filter(p -> "ACTIVE".equals(p.getStatus()) && p.getDeletedAt() == null)
                .map(this::mapToDTO);
    }

    private ProductRecommendationDTO mapToDTO(Product product) {
        ProductRecommendationDTO dto = new ProductRecommendationDTO();
        dto.setId(product.getProductId());
        dto.setName(product.getProductName());

        if (product.getProductDetails() != null && !product.getProductDetails().isEmpty()) {
            ProductDetail firstDetail = product.getProductDetails().get(0);

            // Set price, color, size (lấy từ detail đầu tiên)
            if (firstDetail.getPrice() != null) {
                dto.setPrice(firstDetail.getPrice().doubleValue());
            }
            if (firstDetail.getColor() != null) {
                dto.setColor(firstDetail.getColor().getColorName());
            }
            if (firstDetail.getSize() != null) {
                dto.setSize(firstDetail.getSize().getSizeName());
            }

            // Lấy ảnh: quét qua TẤT CẢ các detail (không chỉ detail đầu tiên),
            // vì ảnh có thể được gắn ở một detail khác (màu/size khác) chứ không phải detail đầu.
            String imagePath = product.getProductDetails().stream()
                    .filter(d -> d.getProductImages() != null && !d.getProductImages().isEmpty())
                    .flatMap(d -> d.getProductImages().stream())
                    .map(ProductImage::getProductImagePath)
                    .filter(p -> p != null && !p.isBlank())
                    .findFirst()
                    .orElse(null);
            dto.setLink(imagePath);
        }

        if (product.getCategory() != null) {
            dto.setCategory(product.getCategory().getCategoryName());
        }

        return dto;
    }
}