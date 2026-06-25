package com.example.backend_fashionstore.QuanLyBanner.dto;

import com.example.backend_fashionstore.entity.Banner;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public class BannerDto {

    @Data
    public static class BannerRequest {
        private String title;
        private String subtitle;
        private MultipartFile imageFile;
        private String linkType;
        private Integer categoryId;
        private Integer productId;
        private String customUrl;
        private Integer displayOrder;
        private String status;
    }

    @Data
    @Builder
    public static class BannerResponse {
        private Integer bannerId;
        private String title;
        private String subtitle;
        private String imagePath;
        private String imageUrl;
        private String linkType;
        private Integer categoryId;
        private String categoryName;
        private Integer productId;
        private String productName;
        private String customUrl;
        private String resolvedUrl;
        private Integer displayOrder;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    public static BannerResponse toResponse(Banner banner) {
        String resolvedUrl = resolveUrl(banner);
        String imageUrl = banner.getImagePath() != null ?
                "/api/" + banner.getImagePath() : null;

        return BannerResponse.builder()
                .bannerId(banner.getBannerId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .imagePath(banner.getImagePath())
                .imageUrl(imageUrl)
                .linkType(banner.getLinkType())
                .categoryId(banner.getCategory() != null ? banner.getCategory().getCategoryId() : null)
                .categoryName(banner.getCategory() != null ? banner.getCategory().getCategoryName() : null)
                .productId(banner.getProduct() != null ? banner.getProduct().getProductId() : null)
                .productName(banner.getProduct() != null ? banner.getProduct().getProductName() : null)
                .customUrl(banner.getCustomUrl())
                .resolvedUrl(resolvedUrl)
                .displayOrder(banner.getDisplayOrder())
                .status(banner.getStatus())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .build();
    }

    private static String resolveUrl(Banner banner) {
        if (banner.getLinkType() == null) return "#";
        return switch (banner.getLinkType()) {
            case "CATEGORY" -> banner.getCategory() != null
                    ? "/shop?categoryId=" + banner.getCategory().getCategoryId()
                    : "/shop";
            case "PRODUCT" -> banner.getProduct() != null
                    ? "/product/" + banner.getProduct().getProductId()
                    : "/shop";
            case "URL" -> banner.getCustomUrl() != null ? banner.getCustomUrl() : "#";
            default -> "#";
        };
    }
}