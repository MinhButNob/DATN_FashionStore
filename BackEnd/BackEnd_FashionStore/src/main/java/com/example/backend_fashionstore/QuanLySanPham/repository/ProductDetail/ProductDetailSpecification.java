// ProductDetailSpecification.java
package com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail;

import com.example.backend_fashionstore.entity.ProductDetail;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductDetailSpecification {

    // ✅ SỬA: Không bị xóa mềm (dùng deletedAt)
    public static Specification<ProductDetail> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    // ✅ SỬA: Còn hoạt động
    public static Specification<ProductDetail> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), "ACTIVE");
    }

    // Lọc theo product_id
    public static Specification<ProductDetail> byProductId(Integer productId) {
        return (root, query, cb) -> {
            if (productId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("product").get("productId"), productId);
        };
    }

    // Lọc theo category_id (qua product)
    public static Specification<ProductDetail> byCategoryId(Integer categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("product").get("category").get("categoryId"), categoryId);
        };
    }

    // Lọc theo color_id
    public static Specification<ProductDetail> byColorId(Integer colorId) {
        return (root, query, cb) -> {
            if (colorId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("color").get("colorId"), colorId);
        };
    }

    // Lọc theo size_id
    public static Specification<ProductDetail> bySizeId(Integer sizeId) {
        return (root, query, cb) -> {
            if (sizeId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("size").get("sizeId"), sizeId);
        };
    }

    // Lọc theo từ khóa (tên sản phẩm, SKU)
    public static Specification<ProductDetail> searchByKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("product").get("productName")), likePattern),
                    cb.like(cb.lower(root.get("sku")), likePattern)
            );
        };
    }

    // Lọc theo khoảng giá
    public static Specification<ProductDetail> byPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) {
                return cb.conjunction();
            }
            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get("price"), minPrice, maxPrice);
            }
            if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
            }
            return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    // Lọc theo số lượng (còn hàng, hết hàng)
    public static Specification<ProductDetail> byStockStatus(String stockStatus) {
        return (root, query, cb) -> {
            if (stockStatus == null || stockStatus.isBlank()) {
                return cb.conjunction();
            }
            return switch (stockStatus.toLowerCase()) {
                case "in_stock" -> cb.greaterThan(root.get("quantity"), 0);
                case "out_of_stock" -> cb.equal(root.get("quantity"), 0);
                case "low_stock" -> cb.between(root.get("quantity"), 1, 5);
                default -> cb.conjunction();
            };
        };
    }

    // Build filter tổng hợp
    public static Specification<ProductDetail> buildFilter(
            Integer productId,
            Integer categoryId,
            Integer colorId,
            Integer sizeId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String stockStatus
    ) {
        return Specification.where(isNotDeleted())
                .and(isActive())
                .and(byProductId(productId))
                .and(byCategoryId(categoryId))
                .and(byColorId(colorId))
                .and(bySizeId(sizeId))
                .and(searchByKeyword(keyword))
                .and(byPriceRange(minPrice, maxPrice))
                .and(byStockStatus(stockStatus));
    }
}