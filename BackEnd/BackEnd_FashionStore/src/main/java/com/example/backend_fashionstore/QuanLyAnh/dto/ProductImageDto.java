package com.example.backend_fashionstore.QuanLyAnh.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

public class ProductImageDto {

    @Data
    public static class ProductImageRequest {
        private Integer productDetailId;
        private String productImageType;
        private Integer displayOrder;
        private Boolean isMain;
        private MultipartFile imageFile;
    }

    @Data
    public static class ProductImageResponse {
        private Integer productImageId;
        private Integer productDetailId;
        private String productImagePath;
        private String productImageType;
        private Integer displayOrder;
        private Boolean isMain;
        private String status;  // ✅ THÊM status
        private LocalDateTime createdAt;
        private String productName;
        private String colorName;
        private String sizeName;
    }

    public static ProductImageResponse toResponse(com.example.backend_fashionstore.entity.ProductImage entity) {
        if (entity == null) return null;

        ProductImageResponse response = new ProductImageResponse();
        response.setProductImageId(entity.getProductImageId());
        response.setProductImagePath(entity.getProductImagePath());
        response.setProductImageType(entity.getProductImageType());
        response.setDisplayOrder(entity.getDisplayOrder());
        response.setIsMain(entity.getIsMain());
        response.setStatus(entity.getStatus());  // ✅ THÊM status
        response.setCreatedAt(entity.getCreatedAt());

        if (entity.getProductDetail() != null) {
            response.setProductDetailId(entity.getProductDetail().getProductDetailId());

            if (entity.getProductDetail().getProduct() != null) {
                response.setProductName(entity.getProductDetail().getProduct().getProductName());
            }

            if (entity.getProductDetail().getColor() != null) {
                response.setColorName(entity.getProductDetail().getColor().getColorName());
            }

            if (entity.getProductDetail().getSize() != null) {
                response.setSizeName(entity.getProductDetail().getSize().getSizeName());
            }
        }

        return response;
    }
}