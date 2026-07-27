package com.example.backend_fashionstore.customer.wishlist.service;

import com.example.backend_fashionstore.auth.repository.CustomerRepository;
import com.example.backend_fashionstore.customer.repository.CustomerProductRepository;
import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import com.example.backend_fashionstore.customer.wishlist.dto.response.WishlistItemResponseDTO;
import com.example.backend_fashionstore.customer.wishlist.dto.response.WishlistResponseDTO;
import com.example.backend_fashionstore.customer.wishlist.repository.WishlistItemRepository;
import com.example.backend_fashionstore.customer.wishlist.repository.WishlistRepository;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.quanLyKhuyenMai.repository.ProductPromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductDetailRepository productDetailRepository;
    private final CustomerProductRepository customerProductRepository;
    private final ProductPromotionRepository productPromotionRepository;

    @Override
    @Transactional(readOnly = true)
    public WishlistResponseDTO getWishlist(Integer customerId) {
        log.info("Fetching wishlist for customer ID: {}", customerId);

        Wishlist wishlist = wishlistRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElse(null);

        if (wishlist == null) {
            return WishlistResponseDTO.builder()
                    .customerId(customerId)
                    .totalItems(0)
                    .items(List.of())
                    .build();
        }

        // Lấy tất cả items chưa bị xóa
        List<WishlistItem> activeItems = wishlistItemRepository
                .findActiveByWishlistId(wishlist.getWishlistId());

        List<WishlistItemResponseDTO> itemDTOs = activeItems.stream()
                .map(this::convertToWishlistItemResponseDTO)
                .collect(Collectors.toList());

        return WishlistResponseDTO.builder()
                .wishlistId(wishlist.getWishlistId())
                .customerId(customerId)
                .totalItems(itemDTOs.size())
                .items(itemDTOs)
                .build();
    }

    @Override
    @Transactional
    public WishlistResponseDTO addToWishlist(Integer customerId, Integer productDetailId) {
        log.info("Adding product detail ID: {} to wishlist for customer ID: {}", productDetailId, customerId);

        // Verify customer exists
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + customerId));

        // Get or create wishlist
        Wishlist wishlist = wishlistRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseGet(() -> {
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setCustomer(customer);
                    newWishlist.setWishlistName("My Wishlist");
                    newWishlist.setIsPublic(false);
                    newWishlist.setWishlistStatus("ACTIVE");
                    newWishlist.setDeletedAt(null);
                    return wishlistRepository.save(newWishlist);
                });

        // ✅ Check if product detail exists - use ProductDetailRepository
        ProductDetail productDetail = productDetailRepository.findByIdWithRelations(productDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm với ID: " + productDetailId));

        // Check if item already exists (including deleted ones)
        var existingItem = wishlistItemRepository
                .findByWishlistIdAndProductDetailId(wishlist.getWishlistId(), productDetailId);

        if (existingItem.isPresent()) {
            WishlistItem item = existingItem.get();
            // If it was deleted, restore it
            if (item.getWishlistItemDeletedAt() != null) {
                item.setWishlistItemDeletedAt(null);
                wishlistItemRepository.save(item);
                log.info("Restored item from wishlist");
            } else {
                log.info("Item already exists in wishlist");
            }
        } else {
            // Create new item
            WishlistItem newItem = new WishlistItem();
            newItem.setWishlist(wishlist);
            newItem.setProductDetail(productDetail);
            newItem.setWishlistItemStatus("ACTIVE");
            newItem.setWishlistItemDeletedAt(null);
            wishlistItemRepository.save(newItem);
            log.info("Added new item to wishlist");
        }

        return getWishlist(customerId);
    }

    @Override
    @Transactional
    public WishlistResponseDTO removeFromWishlist(Integer customerId, Integer productDetailId) {
        log.info("Removing product detail ID: {} from wishlist for customer ID: {}", productDetailId, customerId);

        Wishlist wishlist = wishlistRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy wishlist của khách hàng"));

        WishlistItem item = wishlistItemRepository
                .findActiveItem(wishlist.getWishlistId(), productDetailId)
                .orElseThrow(() -> new RuntimeException("Item không tồn tại trong wishlist"));

        // Soft delete
        item.setWishlistItemDeletedAt(LocalDateTime.now());
        wishlistItemRepository.save(item);
        log.info("Soft deleted item from wishlist");

        return getWishlist(customerId);
    }

    @Override
    @Transactional
    public WishlistResponseDTO hardRemoveFromWishlist(Integer customerId, Integer productDetailId) {
        log.info("Hard removing product detail ID: {} from wishlist for customer ID: {}", productDetailId, customerId);

        Wishlist wishlist = wishlistRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy wishlist của khách hàng"));

        WishlistItem item = wishlistItemRepository
                .findByWishlistIdAndProductDetailId(wishlist.getWishlistId(), productDetailId)
                .orElseThrow(() -> new RuntimeException("Item không tồn tại trong wishlist"));

        // Hard delete - remove from database
        wishlistItemRepository.delete(item);
        log.info("Hard deleted item from wishlist");

        return getWishlist(customerId);
    }

    /**
     * Convert WishlistItem entity to WishlistItemResponseDTO
     * Includes product details, pricing, and discount calculation
     */
    private WishlistItemResponseDTO convertToWishlistItemResponseDTO(WishlistItem item) {
        ProductDetail productDetail = item.getProductDetail();
        Product product = productDetail.getProduct();

        // Get promotion if exists
        Promotion promotion = getPromotionForProductDetail(productDetail);

        // Calculate discounted price
        BigDecimal discountedPrice = calculateDiscountedPrice(productDetail.getPrice(), promotion);

        // Get main image
        String imageUrl = getMainImageUrlForProductDetail(productDetail);

        // Get color and size names from Color and Size entities
        String colorName = productDetail.getColor() != null ? productDetail.getColor().getColorName() : "N/A";
        String sizeName = productDetail.getSize() != null ? productDetail.getSize().getSizeName() : "N/A";

        return WishlistItemResponseDTO.builder()
                .wishlistItemId(item.getWishlistItemId())
                .productDetailId(productDetail.getProductDetailId())
                .productId(product.getProductId())
                .productName(product.getProductName())
                .colorName(colorName)
                .sizeName(sizeName)
                .price(productDetail.getPrice())
                .discountedPrice(discountedPrice)
                .imageUrl(imageUrl)
                .stock(productDetail.getQuantity() != null ? productDetail.getQuantity() : 0)
                .addedAt(item.getWishlistItemAddedAt())
                .build();
    }

    /**
     * Get promotion for a specific product detail
     */
    private Promotion getPromotionForProductDetail(ProductDetail productDetail) {
        if (productDetail == null || productDetail.getProductDetailId() == null) {
            return null;
        }

        List<ProductPromotion> promotions = productPromotionRepository
                .findByProductDetailId(productDetail.getProductDetailId());

        if (promotions == null || promotions.isEmpty()) {
            return null;
        }

        Promotion promotion = promotions.get(0).getPromotion();
        if (promotion != null && promotion.getPromotionDeletedAt() == null &&
                "ACTIVE".equals(promotion.getPromotionStatus())) {
            return promotion;
        }

        return null;
    }

    /**
     * Calculate discounted price based on promotion
     */
    private BigDecimal calculateDiscountedPrice(BigDecimal originalPrice, Promotion promotion) {
        if (promotion == null || originalPrice == null) {
            return originalPrice;
        }

        BigDecimal discountPercent = promotion.getPromotionDiscountPercent();
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) == 0) {
            return originalPrice;
        }

        BigDecimal discount = originalPrice.multiply(
                discountPercent.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        return originalPrice.subtract(discount).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Get main image URL for a product detail
     */
    private String getMainImageUrlForProductDetail(ProductDetail productDetail) {
        if (productDetail == null || productDetail.getProductDetailId() == null) {
            return null;
        }

        try {
            List<ProductImage> images = customerProductRepository
                    .findActiveImagesByVariantId(productDetail.getProductDetailId());

            if (images == null || images.isEmpty()) {
                return null;
            }

            // Find main image first
            ProductImage mainImage = images.stream()
                    .filter(img -> img.getIsMain() != null && img.getIsMain())
                    .findFirst()
                    .orElse(images.get(0));

            String imagePath = mainImage.getProductImagePath();
            if (imagePath == null || imagePath.isEmpty()) {
                return null;
            }

            // Handle different URL formats
            if (imagePath.startsWith("http")) {
                return imagePath;
            } else {
                String cleanPath = imagePath.startsWith("/") ? imagePath : "/" + imagePath;
                return "http://localhost:8080" + cleanPath;
            }
        } catch (Exception e) {
            log.error("Error reading image for product detail ID {}: {}",
                    productDetail.getProductDetailId(), e.getMessage());
            return null;
        }
    }
}