package com.example.backend_fashionstore.customer.wishlist.service;

import com.example.backend_fashionstore.customer.repository.CustomerProductRepository;
import com.example.backend_fashionstore.customer.wishlist.dto.response.WishlistProductResponse;
import com.example.backend_fashionstore.customer.wishlist.repository.WishlistItemRepository;
import com.example.backend_fashionstore.customer.wishlist.repository.WishlistRepository;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.quanLyKhachHang.repository.KhachHangCustomerRepository;
import com.example.backend_fashionstore.quanLyKhuyenMai.repository.ProductPromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final KhachHangCustomerRepository customerRepository;
    private final CustomerProductRepository customerProductRepository;
    private final ProductPromotionRepository productPromotionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WishlistProductResponse> getWishlist(Integer customerId) {
        log.info("Fetching wishlist for customer ID: {}", customerId);
        Optional<Wishlist> wishlistOpt = wishlistRepository.findByCustomer_CustomerIdAndWishlistStatus(customerId, "ACTIVE");
        if (wishlistOpt.isEmpty()) {
            return Collections.emptyList();
        }

        List<WishlistItem> activeItems = wishlistItemRepository.findByWishlist_WishlistIdAndWishlistItemStatus(
                wishlistOpt.get().getWishlistId(), "ACTIVE");

        return activeItems.stream()
                .map(item -> item.getProductDetail().getProduct())
                .distinct()
                .map(this::convertToWishlistProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void toggleWishlist(Integer customerId, Integer productId) {
        log.info("Toggling wishlist for customer ID: {}, product ID: {}", customerId, productId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + customerId));

        Wishlist wishlist = wishlistRepository.findByCustomer_CustomerIdAndWishlistStatus(customerId, "ACTIVE")
                .orElseGet(() -> {
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setCustomer(customer);
                    newWishlist.setWishlistName("Default");
                    newWishlist.setIsPublic(false);
                    newWishlist.setWishlistStatus("ACTIVE");
                    return wishlistRepository.save(newWishlist);
                });

        Optional<WishlistItem> existingItemOpt = wishlistItemRepository
                .findByWishlist_WishlistIdAndProductDetail_Product_ProductIdAndWishlistItemStatus(
                        wishlist.getWishlistId(), productId, "ACTIVE");

        if (existingItemOpt.isPresent()) {
            WishlistItem item = existingItemOpt.get();
            item.setWishlistItemStatus("DELETED");
            item.setWishlistItemDeletedAt(LocalDateTime.now());
            wishlistItemRepository.save(item);
            log.info("Removed product ID: {} from customer ID: {} wishlist", productId, customerId);
        } else {
            List<ProductDetail> details = customerProductRepository.findActiveVariantsByProductId(productId);
            if (details.isEmpty()) {
                throw new RuntimeException("Sản phẩm không có biến thể hoạt động");
            }
            ProductDetail productDetail = details.get(0);

            WishlistItem newItem = new WishlistItem();
            newItem.setWishlist(wishlist);
            newItem.setProductDetail(productDetail);
            newItem.setWishlistItemStatus("ACTIVE");
            wishlistItemRepository.save(newItem);
            log.info("Added product ID: {} to customer ID: {} wishlist", productId, customerId);
        }
    }

    private WishlistProductResponse convertToWishlistProductResponse(Product product) {
        Promotion productPromotion = getPromotionForProduct(product);
        List<ProductDetail> variants = customerProductRepository.findActiveVariantsByProductId(product.getProductId());

        BigDecimal minPrice = BigDecimal.ZERO;
        int totalStock = 0;
        String mainImageUrl = null;

        if (variants != null && !variants.isEmpty()) {
            BigDecimal originalMinPrice = variants.stream()
                    .map(ProductDetail::getPrice)
                    .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            minPrice = calculateDiscountedPrice(originalMinPrice, productPromotion);

            totalStock = variants.stream()
                    .mapToInt(pd -> pd.getQuantity() != null ? pd.getQuantity() : 0)
                    .sum();

            for (ProductDetail variant : variants) {
                try {
                    List<ProductImage> images = customerProductRepository.findActiveImagesByVariantId(variant.getProductDetailId());
                    if (images != null && !images.isEmpty()) {
                        ProductImage mainImage = images.stream()
                                .filter(img -> img.getIsMain() != null && img.getIsMain())
                                .findFirst()
                                .orElse(images.get(0));

                        String imagePath = mainImage.getProductImagePath();
                        if (imagePath != null && !imagePath.isEmpty()) {
                            if (imagePath.startsWith("http")) {
                                mainImageUrl = imagePath;
                            } else {
                                String cleanPath = imagePath.startsWith("/") ? imagePath : "/" + imagePath;
                                mainImageUrl = "http://localhost:8080" + cleanPath;
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error reading image for variant ID {}: {}", variant.getProductDetailId(), e.getMessage());
                }
            }
        }

        BigDecimal price = minPrice;
        BigDecimal oldPrice = null;
        if (productPromotion != null && minPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal originalMinPrice = variants.stream()
                    .map(ProductDetail::getPrice)
                    .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            if (originalMinPrice.compareTo(minPrice) > 0) {
                oldPrice = originalMinPrice;
            }
        }

        return WishlistProductResponse.builder()
                .id(product.getProductId())
                .name(product.getProductName())
                .price(price)
                .oldPrice(oldPrice)
                .rating(4.8)
                .sold(120)
                .image(mainImageUrl)
                .inStock(totalStock > 0)
                .build();
    }

    private Promotion getPromotionForProduct(Product product) {
        if (product.getProductDetails() == null) return null;

        for (ProductDetail detail : product.getProductDetails()) {
            if (detail.getDeletedAt() == null && detail.getProductDetailId() != null) {
                List<ProductPromotion> promotions = productPromotionRepository.findByProductDetailId(detail.getProductDetailId());
                if (!promotions.isEmpty()) {
                    Promotion promo = promotions.get(0).getPromotion();
                    if (promo != null && promo.getPromotionDeletedAt() == null &&
                            "ACTIVE".equals(promo.getPromotionStatus())) {
                        return promo;
                    }
                }
            }
        }
        return null;
    }

    private BigDecimal calculateDiscountedPrice(BigDecimal originalPrice, Promotion promotion) {
        if (promotion == null || originalPrice == null) return originalPrice;
        BigDecimal discountPercent = promotion.getPromotionDiscountPercent();
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) == 0) return originalPrice;

        BigDecimal discount = originalPrice.multiply(discountPercent.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        return originalPrice.subtract(discount).setScale(0, RoundingMode.HALF_UP);
    }
}