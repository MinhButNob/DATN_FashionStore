package com.example.backend_fashionstore.QuanLyAnh.repository;

import com.example.backend_fashionstore.entity.ProductImage;
import org.springframework.data.jpa.domain.Specification;

public class ProductImageSpecification {

    // Lọc theo product_detail_id
    public static Specification<ProductImage> byProductDetailId(Integer productDetailId) {
        return (root, query, cb) -> {
            if (productDetailId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("productDetail").get("productDetailId"), productDetailId);
        };
    }

    // Lọc theo đường dẫn ảnh (tìm kiếm)
    public static Specification<ProductImage> searchByPath(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("productImagePath")), "%" + keyword.toLowerCase() + "%");
        };
    }

    // Lọc theo loại ảnh
    public static Specification<ProductImage> byImageType(String imageType) {
        return (root, query, cb) -> {
            if (imageType == null || imageType.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("productImageType"), imageType);
        };
    }

    // Lọc ảnh chính
    public static Specification<ProductImage> isMain(Boolean isMain) {
        return (root, query, cb) -> {
            if (isMain == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isMain"), isMain);
        };
    }

    // Build filter tổng hợp
    public static Specification<ProductImage> buildFilter(
            Integer productDetailId,
            String keyword,
            String imageType,
            Boolean isMain
    ) {
        return Specification.where(byProductDetailId(productDetailId))
                .and(searchByPath(keyword))
                .and(byImageType(imageType))
                .and(isMain(isMain));
    }
}