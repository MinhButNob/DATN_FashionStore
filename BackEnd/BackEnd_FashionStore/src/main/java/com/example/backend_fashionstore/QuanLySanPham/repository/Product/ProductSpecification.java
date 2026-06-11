package com.example.backend_fashionstore.QuanLySanPham.repository.Product;

import com.example.backend_fashionstore.entity.Product;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    public static Specification<Product> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Product> filterByStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) return cb.conjunction();
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Product> searchByName(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%");
        };
    }

    public static Specification<Product> filterByCategory(Integer categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return cb.conjunction();
            return cb.equal(root.get("category").get("categoryId"), categoryId);
        };
    }

    public static Specification<Product> buildFilter(String keyword, Integer categoryId, String status) {
        return Specification.where(isNotDeleted())
                .and(searchByName(keyword))
                .and(filterByCategory(categoryId))
                .and(filterByStatus(status));
    }
}