package com.example.backend_fashionstore.customer.service;

import com.example.backend_fashionstore.customer.dto.request.ProductFilterCustomerRequest;
import com.example.backend_fashionstore.customer.dto.response.*;
import com.example.backend_fashionstore.customer.repository.CustomerProductRepository;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.quanLyKhuyenMai.repository.ProductPromotionRepository;
import com.example.backend_fashionstore.quanLyKhuyenMai.repository.PromotionRepository;
import com.example.backend_fashionstore.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerProductServiceImpl implements CustomerProductService {

    private final CustomerProductRepository customerProductRepository;
    private final ProductPromotionRepository productPromotionRepository;
    private final PromotionRepository promotionRepository;
    private final ReviewRepository reviewRepository;

    private static final String BASE_IMAGE_URL = "http://localhost:8080";

    @Override
    public HomeProductResponse getProducts(ProductFilterCustomerRequest filter) {
        Sort sort = getSort(filter.getSortBy());
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Page<Product> productPage = customerProductRepository.filterProducts(
                filter.getKeyword(),
                filter.getCategoryId(),
                filter.getColorId(),
                filter.getSizeId(),
                filter.getMinPrice(),
                filter.getMaxPrice(),
                pageable
        );

        List<ProductCardResponse> products = productPage.getContent().stream()
                .map(this::convertToCardResponse)
                .collect(Collectors.toList());

        return HomeProductResponse.builder()
                .products(products)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .build();
    }

    @Override
    public ProductDetailCustomerResponse getProductDetail(Integer productId) {
        Product product = customerProductRepository.findProductDetailById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // ✅ Lấy tất cả variants, mỗi variant tự tính discountedPrice theo KM RIÊNG của nó
        List<ProductDetail> activeVariants = customerProductRepository.findActiveVariantsByProductId(productId);

        List<VariantCustomerResponse> variants = activeVariants.stream()
                .map(this::convertToVariantResponse)
                .collect(Collectors.toList());

        // ✅ Chỉ hiển thị banner "KM chung" nếu TẤT CẢ biến thể đang active cùng chung 1 khuyến mãi
        Promotion productPromotion = getUniformPromotionForVariants(activeVariants);

        // Lấy ảnh từ variant đầu tiên
        List<ImageCustomerResponse> images = Collections.emptyList();
        if (!activeVariants.isEmpty()) {
            images = customerProductRepository
                    .findActiveImagesByVariantId(activeVariants.get(0).getProductDetailId())
                    .stream()
                    .map(this::convertToImageResponse)
                    .collect(Collectors.toList());
        }

        List<ColorOptionResponse> colorOptions = customerProductRepository
                .getAvailableColors(productId)
                .stream()
                .map(this::convertToColorOptionResponse)
                .collect(Collectors.toList());

        List<SizeOptionResponse> sizeOptions = customerProductRepository
                .getAvailableSizes(productId)
                .stream()
                .map(this::convertToSizeOptionResponse)
                .collect(Collectors.toList());

        // Tính khoảng giá (sau khuyến mãi nếu có)
        BigDecimal minPrice = BigDecimal.ZERO;
        BigDecimal maxPrice = BigDecimal.ZERO;

        if (variants != null && !variants.isEmpty()) {
            minPrice = variants.stream()
                    .map(VariantCustomerResponse::getDiscountedPrice)
                    .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            maxPrice = variants.stream()
                    .map(VariantCustomerResponse::getDiscountedPrice)
                    .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }

        int totalStock = variants.stream()
                .mapToInt(v -> v.getQuantity() != null ? v.getQuantity() : 0)
                .sum();

        // ✅ Tạo promotion response
        PromotionSummaryResponse promotionSummary = null;
        if (productPromotion != null) {
            promotionSummary = PromotionSummaryResponse.builder()
                    .promotionId(productPromotion.getPromotionId())
                    .promotionName(productPromotion.getPromotionName())
                    .promotionDiscountPercent(productPromotion.getPromotionDiscountPercent())
                    .build();
        }

        return ProductDetailCustomerResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : null)
                .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                .variants(variants)
                .images(images)
                .colorOptions(colorOptions)
                .sizeOptions(sizeOptions)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .totalStock(totalStock)
                .promotion(promotionSummary)  // ✅ Thêm promotion vào response
                .build();
    }

    // ✅ Chỉ trả về 1 promotion nếu TẤT CẢ biến thể (còn active) đang dùng CHUNG đúng 1 khuyến mãi đó
    // Dùng để hiển thị banner "Giảm X% cho tất cả biến thể" - không được suy diễn từ 1 variant bất kỳ.
    private Promotion getUniformPromotionForVariants(List<ProductDetail> activeVariants) {
        if (activeVariants == null || activeVariants.isEmpty()) return null;

        Promotion common = null;
        for (ProductDetail detail : activeVariants) {
            Promotion effective = getSingleActivePromotion(detail.getProductDetailId());
            if (effective == null) return null; // có biến thể không có KM -> không đồng nhất
            if (common == null) {
                common = effective;
            } else if (!common.getPromotionId().equals(effective.getPromotionId())) {
                return null; // các biến thể dùng KM khác nhau -> không đồng nhất
            }
        }
        return common;
    }

    // ✅ Lấy khuyến mãi (còn hiệu lực) đang gán cho 1 biến thể cụ thể
    private Promotion getSingleActivePromotion(Integer productDetailId) {
        if (productDetailId == null) return null;
        List<ProductPromotion> promotions = productPromotionRepository.findByProductDetailId(productDetailId);
        for (ProductPromotion pp : promotions) {
            Promotion promo = pp.getPromotion();
            if (promo != null && promo.getPromotionDeletedAt() == null &&
                    "ACTIVE".equals(promo.getPromotionStatus())) {
                return promo;
            }
        }
        return null;
    }

    // ✅ Tính giá sau khuyến mãi theo ĐÚNG khuyến mãi riêng của từng biến thể
    // (đồng bộ với logic bên admin - QuanLySanPham.ProductService#calculateVariantDiscountedPrice)
    private BigDecimal calculateDiscountedPrice(ProductDetail detail) {
        if (detail == null || detail.getPrice() == null) return detail != null ? detail.getPrice() : BigDecimal.ZERO;

        List<ProductPromotion> promotions = productPromotionRepository
                .findByProductDetailId(detail.getProductDetailId());
        if (promotions == null || promotions.isEmpty()) return detail.getPrice();

        int totalDiscountPercent = 0;
        for (ProductPromotion pp : promotions) {
            Promotion promo = pp.getPromotion();
            if (promo != null && promo.getPromotionDeletedAt() == null &&
                    "ACTIVE".equals(promo.getPromotionStatus()) &&
                    promo.getPromotionDiscountPercent() != null) {
                totalDiscountPercent += promo.getPromotionDiscountPercent().intValue();
            }
        }

        if (totalDiscountPercent <= 0) return detail.getPrice();
        if (totalDiscountPercent > 100) totalDiscountPercent = 100;

        BigDecimal discount = detail.getPrice()
                .multiply(BigDecimal.valueOf(totalDiscountPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return detail.getPrice().subtract(discount).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public HomeProductResponse getLatestProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> productPage = customerProductRepository.findLatestProducts(pageable);

        List<ProductCardResponse> products = productPage.getContent().stream()
                .map(this::convertToCardResponse)
                .collect(Collectors.toList());

        return HomeProductResponse.builder()
                .products(products)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .build();
    }

    @Override
    public HomeProductResponse getProductsByCategory(Integer categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> productPage = customerProductRepository.findProductsByCategory(categoryId, pageable);

        List<ProductCardResponse> products = productPage.getContent().stream()
                .map(this::convertToCardResponse)
                .collect(Collectors.toList());

        return HomeProductResponse.builder()
                .products(products)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .build();
    }

    @Override
    public HomeProductResponse getFeaturedProducts(int page, int size) {
        // ✅ CHỈ lấy sản phẩm bán chạy THẬT dựa trên số lượng đã bán trong order_details.
        // Không còn fallback bổ sung sản phẩm mới nhất (dữ liệu fake) khi chưa đủ số lượng —
        // nếu chưa có đơn hàng thật nào, danh sách sẽ trống và section sẽ tự ẩn ở FE.
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> bestSellingPage = customerProductRepository.findRealBestSellingProducts(pageable);

        List<ProductCardResponse> products = bestSellingPage.getContent().stream()
                .map(this::convertToCardResponse)
                .collect(Collectors.toList());

        return HomeProductResponse.builder()
                .products(products)
                .page(bestSellingPage.getNumber())
                .size(bestSellingPage.getSize())
                .totalElements(bestSellingPage.getTotalElements())
                .totalPages(bestSellingPage.getTotalPages())
                .build();
    }

    @Override
    public HomeProductResponse getTopRatedProducts(int page, int size) {
        // ✅ CHỈ lấy sản phẩm đã có người mua VÀ đánh giá thật (review APPROVED),
        // sắp xếp theo điểm đánh giá trung bình cao nhất trước (đúng nghĩa "đánh giá cao").
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> reviewedPage = customerProductRepository.findTopRatedProducts(pageable);

        List<ProductCardResponse> products = reviewedPage.getContent().stream()
                .map(this::convertToCardResponse)
                .collect(Collectors.toList());

        return HomeProductResponse.builder()
                .products(products)
                .page(reviewedPage.getNumber())
                .size(reviewedPage.getSize())
                .totalElements(reviewedPage.getTotalElements())
                .totalPages(reviewedPage.getTotalPages())
                .build();
    }

    private ProductCardResponse convertToCardResponse(Product product) {
        List<ProductDetail> variants = customerProductRepository.findActiveVariantsByProductId(product.getProductId());

        BigDecimal minPrice = BigDecimal.ZERO;
        BigDecimal maxPrice = BigDecimal.ZERO;
        int totalStock = 0;
        String mainImageUrl = null;

        if (variants != null && !variants.isEmpty()) {
            // ✅ Tính giá sau khuyến mãi theo TỪNG biến thể rồi mới lấy min/max
            // (không dùng chung 1 promotion cho cả sản phẩm - mỗi biến thể có thể có KM khác nhau)
            List<BigDecimal> discountedPrices = variants.stream()
                    .map(this::calculateDiscountedPrice)
                    .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.toList());

            minPrice = discountedPrices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            maxPrice = discountedPrices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            totalStock = variants.stream()
                    .mapToInt(pd -> pd.getQuantity() != null ? pd.getQuantity() : 0)
                    .sum();

            // Lấy ảnh
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
                                mainImageUrl = BASE_IMAGE_URL + cleanPath;
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("Lỗi lấy ảnh cho variant {}: {}", variant.getProductDetailId(), e.getMessage());
                }
            }
        }

        if (mainImageUrl == null) {
            mainImageUrl = "https://via.placeholder.com/300x300?text=No+Image";
        }

        List<String> colorOptions = Collections.emptyList();
        try {
            colorOptions = customerProductRepository.getAvailableColors(product.getProductId())
                    .stream()
                    .map(Color::getColorName)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Lỗi lấy màu sắc cho sản phẩm {}: {}", product.getProductId(), e.getMessage());
        }

        // ✅ Số lượng đã bán thật + đánh giá thật (không fake)
        Integer soldCount = 0;
        try {
            soldCount = customerProductRepository.getSoldCountByProductId(product.getProductId());
        } catch (Exception e) {
            log.error("Lỗi lấy số lượng đã bán cho sản phẩm {}: {}", product.getProductId(), e.getMessage());
        }

        Double averageRating = 0.0;
        Integer reviewCount = 0;
        try {
            averageRating = reviewRepository.getAverageRatingByProductId(product.getProductId());
            reviewCount = reviewRepository.getTotalReviewsByProductId(product.getProductId());
        } catch (Exception e) {
            log.error("Lỗi lấy đánh giá cho sản phẩm {}: {}", product.getProductId(), e.getMessage());
        }

        return ProductCardResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .mainImageUrl(mainImageUrl)
                .colorOptions(colorOptions)
                .totalStock(totalStock)
                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : null)
                .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                .variants(variants != null
                        ? variants.stream()
                        .map(this::convertToVariantResponse)
                        .collect(Collectors.toList())
                        : Collections.emptyList())
                .soldCount(soldCount != null ? soldCount : 0)
                .averageRating(averageRating != null ? averageRating : 0.0)
                .reviewCount(reviewCount != null ? reviewCount : 0)
                .build();
    }

    // ✅ Mỗi biến thể tự tính giá sau KM theo đúng khuyến mãi riêng của nó
    private VariantCustomerResponse convertToVariantResponse(ProductDetail detail) {
        BigDecimal discountedPrice = calculateDiscountedPrice(detail);

        return VariantCustomerResponse.builder()
                .productDetailId(detail.getProductDetailId())
                .sku(detail.getSku())
                .colorId(detail.getColor() != null ? detail.getColor().getColorId() : null)
                .colorName(detail.getColor() != null ? detail.getColor().getColorName() : null)
                .colorCode(detail.getColor() != null ? detail.getColor().getColorCode() : null)
                .hexCode(detail.getColor() != null ? detail.getColor().getHexCode() : null)
                .sizeId(detail.getSize() != null ? detail.getSize().getSizeId() : null)
                .sizeName(detail.getSize() != null ? detail.getSize().getSizeName() : null)
                .sizeCode(detail.getSize() != null ? detail.getSize().getSizeCode() : null)
                .price(detail.getPrice())
                .discountedPrice(discountedPrice)  // ✅ Thêm giá sau khuyến mãi
                .quantity(detail.getQuantity() != null ? detail.getQuantity() : 0)
                .inStock(detail.getQuantity() != null && detail.getQuantity() > 0)
                .build();
    }

    private ImageCustomerResponse convertToImageResponse(ProductImage image) {
        String imagePath = image.getProductImagePath();
        String fullImageUrl;

        if (imagePath != null && imagePath.startsWith("http")) {
            fullImageUrl = imagePath;
        } else {
            String cleanPath = imagePath;
            if (cleanPath != null && !cleanPath.startsWith("/")) {
                cleanPath = "/" + cleanPath;
            }
            fullImageUrl = BASE_IMAGE_URL + (cleanPath != null ? cleanPath : "");
        }

        return ImageCustomerResponse.builder()
                .productImageId(image.getProductImageId())
                .imageUrl(fullImageUrl)
                .isMain(image.getIsMain() != null && image.getIsMain())
                .displayOrder(image.getDisplayOrder() != null ? image.getDisplayOrder() : 0)
                .build();
    }

    private ColorOptionResponse convertToColorOptionResponse(Color color) {
        return ColorOptionResponse.builder()
                .colorId(color.getColorId())
                .colorName(color.getColorName())
                .colorCode(color.getColorCode())
                .hexCode(color.getHexCode())
                .build();
    }

    private SizeOptionResponse convertToSizeOptionResponse(Size size) {
        return SizeOptionResponse.builder()
                .sizeId(size.getSizeId())
                .sizeName(size.getSizeName())
                .sizeCode(size.getSizeCode())
                .build();
    }

    private Sort getSort(String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            return Sort.by("createdAt").descending();
        }

        switch (sortBy) {
            case "price_asc":
                return Sort.by("minPrice").ascending();
            case "price_desc":
                return Sort.by("maxPrice").descending();
            case "name_asc":
                return Sort.by("productName").ascending();
            case "name_desc":
                return Sort.by("productName").descending();
            case "newest":
                return Sort.by("createdAt").descending();
            default:
                return Sort.by("createdAt").descending();
        }
    }
}