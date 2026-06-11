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

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Integer id) {
        // ✅ THÊM VALIDATION
        if (id == null) {
            throw new IllegalArgumentException("ID sản phẩm không được để trống");
        }

        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return toResponse(product);
    }

    public ProductResponse createProduct(ProductCreateRequest request) {
        // ✅ THÊM VALIDATION
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

                updatePromotionForProductDetail(savedDetail.getProductDetailId(), v.getPromotionId());
            }
        }

        log.info("Tạo sản phẩm thành công với ID: {}", product.getProductId());
        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Integer id, ProductCreateRequest request) {
        // ✅ THÊM VALIDATION - QUAN TRỌNG NHẤT ĐỂ FIX LỖI
        log.info("Updating product with id: {}", id);

        if (id == null) {
            log.error("Product ID is null in updateProduct method");
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

                    updatePromotionForProductDetail(existing.getProductDetailId(), v.getPromotionId());

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

                    updatePromotionForProductDetail(savedDetail.getProductDetailId(), v.getPromotionId());

                    updatedDetails.add(savedDetail);
                }
            }
        }

        for (ProductDetail oldDetail : existingMap.values()) {
            oldDetail.setDeletedAt(LocalDateTime.now());
            oldDetail.setStatus("DELETED");

            removeAllPromotionsFromProductDetail(oldDetail.getProductDetailId());

            updatedDetails.add(oldDetail);
        }

        product.setProductDetails(updatedDetails);
        Product updated = productRepository.save(product);
        log.info("Cập nhật sản phẩm thành công với ID: {}", id);
        return toResponse(updated);
    }

    public void deleteProduct(Integer id) {
        // ✅ THÊM VALIDATION
        if (id == null) {
            throw new IllegalArgumentException("ID sản phẩm không được để trống");
        }

        int deletedCount = productRepository.softDelete(id, LocalDateTime.now());
        if (deletedCount == 0) {
            throw new ResourceNotFoundException("Product not found or already deleted with id: " + id);
        }
        productDetailRepository.softDeleteByProductId(id);
        log.info("Xóa mềm sản phẩm thành công với ID: {}", id);
    }

    public ProductResponse toggleStatus(Integer id) {
        // ✅ THÊM VALIDATION
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

    // Cập nhật promotion cho product detail
    private void updatePromotionForProductDetail(Integer productDetailId, Integer promotionId) {
        if (productDetailId == null) {
            log.warn("ProductDetailId is null, cannot update promotion");
            return;
        }

        removeAllPromotionsFromProductDetail(productDetailId);

        if (promotionId != null) {
            try {
                Promotion promotion = promotionRepository.findById(promotionId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi với id: " + promotionId));

                LocalDateTime now = LocalDateTime.now();
                if (promotion.getPromotionDeletedAt() == null &&
                        "ACTIVE".equals(promotion.getPromotionStatus()) &&
                        now.isAfter(promotion.getPromotionStartDate()) &&
                        now.isBefore(promotion.getPromotionEndDate())) {

                    ProductDetail productDetail = productDetailRepository.findById(productDetailId)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy product detail với id: " + productDetailId));

                    ProductPromotion productPromotion = ProductPromotion.builder()
                            .productDetail(productDetail)
                            .promotion(promotion)
                            .build();

                    productPromotionRepository.save(productPromotion);
                    log.info("Đã gán promotion {} cho product detail {}", promotionId, productDetailId);
                }
            } catch (Exception e) {
                log.error("Lỗi khi gán promotion cho product detail {}: {}", productDetailId, e.getMessage());
            }
        }
    }

    // Xóa tất cả promotion của product detail
    private void removeAllPromotionsFromProductDetail(Integer productDetailId) {
        if (productDetailId == null) {
            log.warn("ProductDetailId is null, cannot remove promotions");
            return;
        }

        try {
            productPromotionRepository.deleteByProductDetailId(productDetailId);
        } catch (Exception e) {
            log.error("Lỗi khi xóa promotion của product detail {}: {}", productDetailId, e.getMessage());
        }
    }

    private ProductResponse toResponse(Product p) {
        if (p == null) {
            return null;
        }

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
                .variants(details.stream()
                        .filter(d -> d.getDeletedAt() == null)
                        .map(d -> ProductResponse.VariantSummary.builder()
                                .productDetailId(d.getProductDetailId())
                                .sku(d.getSku())
                                .color(d.getColor() != null ? d.getColor().getColorName() : null)
                                .size(d.getSize() != null ? d.getSize().getSizeName() : null)
                                .price(d.getPrice())
                                .quantity(d.getQuantity())
                                .status(d.getStatus())
                                .isLowStock(d.getQuantity() != null && d.getQuantity() <= LOW_STOCK_THRESHOLD)
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}