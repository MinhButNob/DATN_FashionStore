package com.example.backend_fashionstore.QuanLySanPham.service;

import com.example.backend_fashionstore.QuanLyDanhMuc.repository.CategoryRepository;
import com.example.backend_fashionstore.QuanLyMauSac.repository.ColorRepository;
import com.example.backend_fashionstore.QuanLySanPham.dto.request.ProductCreateRequest;
import com.example.backend_fashionstore.QuanLySanPham.dto.request.ProductFilterRequest;
import com.example.backend_fashionstore.QuanLySanPham.dto.response.PageResponse;
import com.example.backend_fashionstore.QuanLySanPham.dto.response.ProductResponse;
import com.example.backend_fashionstore.QuanLySanPham.dto.response.ProductStatsResponse;
import com.example.backend_fashionstore.QuanLySanPham.exception.ResourceNotFoundException;
import com.example.backend_fashionstore.QuanLySanPham.repository.Product.ProductRepository;
import com.example.backend_fashionstore.QuanLySanPham.repository.Product.ProductSpecification;
import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import com.example.backend_fashionstore.QuanLySize.repository.SizeRepository;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.quanLyKhuyenMai.repository.ProductPromotionRepository;
import com.example.backend_fashionstore.quanLyKhuyenMai.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final ProductDetailRepository productDetailRepository;
    private final PromotionRepository promotionRepository;
    private final ProductPromotionRepository productPromotionRepository;

    private static final int LOW_STOCK_THRESHOLD = 10;

    // ==================== GET PRODUCTS ====================
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(ProductFilterRequest filter) {
        var pageable = PageRequest.of(filter.getPage(), filter.getSize(),
                Sort.by(Sort.Direction.fromString(filter.getSortDir()), filter.getSortBy()));

        var spec = ProductSpecification.buildFilter(
                filter.getKeyword(),
                filter.getCategoryId(),
                filter.getStatus()
        );

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<Integer> productIds = productPage.getContent().stream()
                .map(Product::getProductId)
                .collect(Collectors.toList());

        if (productIds.isEmpty()) {
            return PageResponse.from(Page.empty(pageable));
        }

        List<Product> productsWithDetails = productRepository.findAllWithDetailsByIds(productIds);

        List<ProductResponse> responses = productsWithDetails.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<ProductResponse>builder()
                .content(responses)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .build();
    }

    // ==================== GET STATISTICS ====================
    @Transactional(readOnly = true)
    public ProductStatsResponse getStatistics() {
        return ProductStatsResponse.builder()
                .totalProducts(productRepository.countActiveProducts())
                .inStockVariants(productRepository.countInStockVariants())
                .lowStockVariants(productRepository.countLowStockVariants(LOW_STOCK_THRESHOLD))
                .lowStockItems(productRepository.findLowStockVariants(LOW_STOCK_THRESHOLD).stream()
                        .map(pd -> ProductStatsResponse.LowStockItem.builder()
                                .productName(pd.getProduct().getProductName())
                                .sku(pd.getSku())
                                .currentStock(pd.getQuantity())
                                .threshold(LOW_STOCK_THRESHOLD)
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    // ==================== GET PRODUCT BY ID ====================
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID sản phẩm không được để trống");
        }

        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        return toResponse(product);
    }

    // ==================== CREATE PRODUCT ====================
    public ProductResponse createProduct(ProductCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request không được để trống");
        }
        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("Category ID không được để trống");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        String status = request.getStatus() != null ? request.getStatus() : "ACTIVE";

        Product product = Product.builder()
                .productName(request.getProductName())
                .description(request.getDescription())
                .category(category)
                .status(status)
                .productDetails(new ArrayList<>())
                .build();

        product = productRepository.save(product);

        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            for (ProductCreateRequest.VariantRequest v : request.getVariants()) {
                Color color = colorRepository.findById(v.getColorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Color not found with id: " + v.getColorId()));
                Size size = sizeRepository.findById(v.getSizeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Size not found with id: " + v.getSizeId()));

                ProductDetail detail = ProductDetail.builder()
                        .product(product)
                        .color(color)
                        .size(size)
                        .sku(v.getSku())
                        .price(v.getPrice())
                        .quantity(v.getQuantity())
                        .status("ACTIVE")
                        .build();

                ProductDetail savedDetail = productDetailRepository.save(detail);
                product.getProductDetails().add(savedDetail);

                // ✅ ÁP DỤNG NHIỀU PROMOTION RIÊNG CHO TỪNG VARIANT
                if (v.getPromotionIds() != null && !v.getPromotionIds().isEmpty()) {
                    applyPromotionsToVariant(savedDetail, v.getPromotionIds());
                }
            }
        }

        // ✅ Áp dụng promotion toàn sản phẩm (chỉ variant KHÔNG có KM riêng)
        if (request.getPromotionId() != null) {
            applyGlobalPromotionToVariants(product, request.getPromotionId());
        }

        log.info("Tạo sản phẩm thành công với ID: {}", product.getProductId());

        return toResponse(product);
    }

    // ==================== UPDATE PRODUCT ====================
    @Transactional
    public ProductResponse updateProduct(Integer id, ProductCreateRequest request) {
        log.info("Updating product with id: {}", id);

        if (id == null) {
            throw new IllegalArgumentException("ID sản phẩm không được để trống");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request không được để trống");
        }

        Product product = productRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        List<ProductDetail> existingDetails = productDetailRepository.findByProductId(id);

        Map<String, ProductDetail> existingMap = existingDetails.stream()
                .collect(Collectors.toMap(
                        d -> d.getColor().getColorId() + "_" + d.getSize().getSizeId(),
                        d -> d
                ));

        List<ProductDetail> updatedDetails = new ArrayList<>();

        if (request.getVariants() != null) {
            for (ProductCreateRequest.VariantRequest v : request.getVariants()) {
                String key = v.getColorId() + "_" + v.getSizeId();

                if (existingMap.containsKey(key)) {
                    ProductDetail existing = existingMap.get(key);
                    existing.setPrice(v.getPrice());
                    existing.setQuantity(v.getQuantity());
                    existing.setSku(v.getSku());
                    existing.setDeletedAt(null);
                    existing.setStatus("ACTIVE");

                    // ✅ CẬP NHẬT PROMOTIONS RIÊNG CHO VARIANT
                    updateVariantPromotions(existing, v.getPromotionIds());

                    updatedDetails.add(existing);
                    existingMap.remove(key);
                } else {
                    Color color = colorRepository.findById(v.getColorId())
                            .orElseThrow(() -> new ResourceNotFoundException("Color not found with id: " + v.getColorId()));
                    Size size = sizeRepository.findById(v.getSizeId())
                            .orElseThrow(() -> new ResourceNotFoundException("Size not found with id: " + v.getSizeId()));

                    ProductDetail newDetail = ProductDetail.builder()
                            .product(product)
                            .color(color)
                            .size(size)
                            .sku(v.getSku())
                            .price(v.getPrice())
                            .quantity(v.getQuantity())
                            .status("ACTIVE")
                            .build();

                    ProductDetail savedDetail = productDetailRepository.save(newDetail);

                    // ✅ ÁP DỤNG PROMOTIONS RIÊNG CHO VARIANT MỚI
                    if (v.getPromotionIds() != null && !v.getPromotionIds().isEmpty()) {
                        applyPromotionsToVariant(savedDetail, v.getPromotionIds());
                    }

                    updatedDetails.add(savedDetail);
                }
            }
        }

        // Xóa variant cũ
        for (ProductDetail oldDetail : existingMap.values()) {
            oldDetail.setDeletedAt(LocalDateTime.now());
            oldDetail.setStatus("DELETED");
            productPromotionRepository.deleteByProductDetailId(oldDetail.getProductDetailId());
            updatedDetails.add(oldDetail);
        }

        product.setProductDetails(updatedDetails);

        // ⚠️ ĐÃ BỎ: gọi removeGlobalPromotionFromAllVariants(product) tại đây trước làm XÓA MẤT
        // các khuyến mãi riêng vừa gán cho biến thể ở trên (vì hàm đó chỉ đoán "biến thể có đúng 1
        // khuyến mãi" là khuyến mãi toàn sản phẩm rồi xóa đi, kể cả khi đó là khuyến mãi riêng vừa lưu).
        // Tính năng "khuyến mãi áp cho toàn sản phẩm" hiện chưa có UI để chọn (form.promotionId luôn null),
        // nên request.getPromotionId() luôn null và đoạn dưới sẽ không chạy - giữ lại để tương thích tương lai.
        if (request.getPromotionId() != null) {
            applyGlobalPromotionToVariants(product, request.getPromotionId());
        }

        Product updated = productRepository.save(product);
        log.info("Cập nhật sản phẩm thành công với ID: {}", id);

        return toResponse(updated);
    }

    // ==================== DELETE PRODUCT ====================
    public void deleteProduct(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID sản phẩm không được để trống");
        }

        Product product = productRepository.findByIdAndNotDeleted(id).orElse(null);
        if (product != null) {
            removeAllPromotionsFromProduct(product);
        }

        int deletedCount = productRepository.softDelete(id, LocalDateTime.now());
        if (deletedCount == 0) {
            throw new ResourceNotFoundException("Product not found or already deleted with id: " + id);
        }
        productDetailRepository.softDeleteByProductId(id);
        log.info("Xóa mềm sản phẩm thành công với ID: {}", id);
    }

    // ==================== TOGGLE STATUS ====================
    public ProductResponse toggleStatus(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID sản phẩm không được để trống");
        }

        Product product = productRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        String newStatus = "ACTIVE".equals(product.getStatus()) ? "INACTIVE" : "ACTIVE";
        product.setStatus(newStatus);

        Product saved = productRepository.save(product);
        log.info("Chuyển trạng thái sản phẩm ID: {} thành: {}", id, newStatus);

        return toResponse(saved);
    }

    // ==================== RESTORE PRODUCT ====================
    @Transactional
    public ProductResponse restoreProduct(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID sản phẩm không được để trống");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (product.getDeletedAt() == null) {
            throw new RuntimeException("Sản phẩm chưa bị xóa, không cần khôi phục");
        }

        product.setStatus("ACTIVE");
        product.setDeletedAt(null);
        product = productRepository.save(product);

        for (ProductDetail detail : product.getProductDetails()) {
            if (detail.getDeletedAt() != null) {
                detail.setStatus("ACTIVE");
                detail.setDeletedAt(null);
                productDetailRepository.save(detail);
            }
        }

        log.info("Khôi phục sản phẩm thành công với ID: {}", id);

        return toResponse(product);
    }

    // ==================== PROMOTION HELPERS ====================

    /**
     * ✅ Áp dụng nhiều promotion riêng cho 1 variant
     */
    private void applyPromotionsToVariant(
            ProductDetail detail,
            List<Integer> promotionIds) {

        // hủy toàn bộ KM cũ
        productPromotionRepository.deleteByProductDetailId(
                detail.getProductDetailId()
        );

        // không chọn gì => hủy KM
        if (promotionIds == null
                || promotionIds.isEmpty()) {

            log.info(
                    "Đã hủy khuyến mãi của variant {}",
                    detail.getProductDetailId()
            );

            return;
        }

        // chỉ lấy khuyến mãi cuối cùng
        Integer promotionId =
                promotionIds.get(
                        promotionIds.size() - 1
                );

        Promotion promotion =
                promotionRepository
                        .findById(promotionId)
                        .orElse(null);

        if (promotion == null) {
            return;
        }

        ProductPromotion pp =
                ProductPromotion.builder()
                        .productDetail(detail)
                        .promotion(promotion)
                        .build();

        productPromotionRepository.save(pp);

        log.info(
                "Variant {} áp dụng promotion {}",
                detail.getProductDetailId(),
                promotionId
        );
    }

    /**
     * ✅ Cập nhật promotions riêng cho variant (giữ nguyên nếu null)
     */
    private void updateVariantPromotions(ProductDetail detail,
                                         List<Integer> newPromotionIds) {

        // null = giữ nguyên
        if (newPromotionIds == null) {
            return;
        }

        // [] = xóa hết
        applyPromotionsToVariant(
                detail,
                newPromotionIds
        );
    }

    /**
     * ✅ Áp dụng promotion toàn sản phẩm CHỈ cho variant KHÔNG có KM riêng
     */
    private void applyGlobalPromotionToVariants(Product product, Integer promotionId) {
        if (promotionId == null || product.getProductDetails() == null) return;

        Promotion promotion = promotionRepository.findById(promotionId).orElse(null);
        if (promotion == null || promotion.getPromotionDeletedAt() != null) return;

        LocalDateTime now = LocalDateTime.now();
        boolean statusOk = "ACTIVE".equals(promotion.getPromotionStatus());
        boolean started = promotion.getPromotionStartDate() == null
                || !now.isBefore(promotion.getPromotionStartDate());
        boolean notEnded = promotion.getPromotionEndDate() == null
                || !now.isAfter(promotion.getPromotionEndDate());

        if (!statusOk || !started || !notEnded) {
            log.warn("Promotion {} không hợp lệ hoặc đã hết hạn (statusOk={}, started={}, notEnded={}, now={}, start={}, end={})",
                    promotionId, statusOk, started, notEnded, now,
                    promotion.getPromotionStartDate(), promotion.getPromotionEndDate());
            return;
        }

        int appliedCount = 0;
        for (ProductDetail detail : product.getProductDetails()) {
            if (detail.getDeletedAt() == null) {
                List<ProductPromotion> existingPromos = productPromotionRepository
                        .findByProductDetailId(detail.getProductDetailId());

                // Chỉ áp dụng nếu variant CHƯA có promotion riêng
                if (existingPromos.isEmpty()) {
                    ProductPromotion pp = ProductPromotion.builder()
                            .productDetail(detail)
                            .promotion(promotion)
                            .build();
                    productPromotionRepository.save(pp);
                    appliedCount++;
                }
            }
        }
        log.info("Đã áp dụng promotion toàn SP {} cho {} variants (không có KM riêng)",
                promotionId, appliedCount);
    }

    /**
     * ✅ Xóa tất cả promotion khỏi sản phẩm (khi delete)
     */
    private void removeAllPromotionsFromProduct(Product product) {
        if (product.getProductDetails() == null) return;
        for (ProductDetail detail : product.getProductDetails()) {
            if (detail.getDeletedAt() == null) {
                productPromotionRepository.deleteByProductDetailId(detail.getProductDetailId());
            }
        }
    }

    // ==================== PRICE CALCULATION ====================

    /**
     * ✅ Tính giá sau khi áp dụng TẤT CẢ khuyến mãi của variant
     */
    private BigDecimal calculateVariantDiscountedPrice(ProductDetail detail) {
        if (detail.getPrice() == null) return BigDecimal.ZERO;

        List<ProductPromotion> promotions = productPromotionRepository
                .findByProductDetailId(detail.getProductDetailId());

        if (promotions == null || promotions.isEmpty()) return detail.getPrice();

        int totalDiscountPercent = 0;
        for (ProductPromotion pp : promotions) {
            if (pp.getPromotion() != null && pp.getPromotion().getPromotionDiscountPercent() != null) {
                totalDiscountPercent += pp.getPromotion().getPromotionDiscountPercent().intValue();
            }
        }

        if (totalDiscountPercent == 0) return detail.getPrice();
        if (totalDiscountPercent > 100) totalDiscountPercent = 100;

        return detail.getPrice().multiply(
                BigDecimal.ONE.subtract(
                        BigDecimal.valueOf(totalDiscountPercent).divide(BigDecimal.valueOf(100))
                )
        );
    }

    // ==================== TO RESPONSE ====================

    /**
     * ✅ Chuyển đổi Product -> ProductResponse (có promotions cho từng variant)
     */
    private ProductResponse toResponse(Product p) {
        if (p == null) return null;

        List<ProductDetail> details = p.getProductDetails();
        if (details == null) details = new ArrayList<>();

        int totalStock = details.stream()
                .filter(d -> d.getDeletedAt() == null)
                .mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 0)
                .sum();

        return ProductResponse.builder()
                .productId(p.getProductId())
                .productName(p.getProductName())
                .description(p.getDescription())
                .status(p.getStatus())
                .totalStock(totalStock)
                .createdAt(p.getCreatedAt())
                .category(p.getCategory() != null
                        ? ProductResponse.CategorySummary.builder()
                        .categoryId(p.getCategory().getCategoryId())
                        .categoryName(p.getCategory().getCategoryName())
                        .build()
                        : null)
                .promotion(null)
                .variants(details.stream()
                        .filter(d -> d.getDeletedAt() == null)
                        .map(d -> {
                            // ✅ Lấy tất cả promotions của variant
                            List<ProductPromotion> variantPromos = productPromotionRepository
                                    .findByProductDetailId(d.getProductDetailId());

                            // ✅ Tính giá sau KM
                            BigDecimal discountedPrice = calculateVariantDiscountedPrice(d);

                            return ProductResponse.VariantSummary.builder()
                                    .productDetailId(d.getProductDetailId())
                                    .sku(d.getSku())
                                    .color(d.getColor() != null ? d.getColor().getColorName() : null)
                                    .size(d.getSize() != null ? d.getSize().getSizeName() : null)
                                    .price(d.getPrice())
                                    .discountedPrice(discountedPrice)
                                    .quantity(d.getQuantity())
                                    .status(d.getStatus())
                                    .isLowStock(d.getQuantity() != null && d.getQuantity() <= LOW_STOCK_THRESHOLD)
                                    // ✅ Trả về danh sách promotions của variant
                                    .promotions(variantPromos.stream()
                                            .map(pp -> ProductResponse.PromotionSummary.builder()
                                                    .promotionId(pp.getPromotion().getPromotionId())
                                                    .promotionName(pp.getPromotion().getPromotionName())
                                                    .promotionDiscountPercent(pp.getPromotion().getPromotionDiscountPercent())
                                                    .build())
                                            .collect(Collectors.toList()))
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .build();
    }
}