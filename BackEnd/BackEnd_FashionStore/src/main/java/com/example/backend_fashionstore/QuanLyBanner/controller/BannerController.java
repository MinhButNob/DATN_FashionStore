package com.example.backend_fashionstore.QuanLyBanner.controller;

import com.example.backend_fashionstore.QuanLyBanner.dto.BannerDto;
import com.example.backend_fashionstore.QuanLyBanner.service.BannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class BannerController {

    private final BannerService bannerService;

    // ========== CLIENT ==========
    @GetMapping("/banners/active")
    public ResponseEntity<?> getActiveBanners() {
        try {
            List<BannerDto.BannerResponse> banners = bannerService.getActiveBanners();
            return ResponseEntity.ok(banners);
        } catch (Exception e) {
            log.error("Error getting active banners: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể tải banner: " + e.getMessage()));
        }
    }

    // ========== ADMIN ==========
    @GetMapping("/admin/banners")
    public ResponseEntity<?> getBanners(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "bannerId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<BannerDto.BannerResponse> banners = bannerService.getBanners(
                    keyword, sortBy, sortDirection, page, size
            );
            return ResponseEntity.ok(banners);
        } catch (Exception e) {
            log.error("Error getting banners: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể tải danh sách banner: " + e.getMessage()));
        }
    }

    @PostMapping("/admin/banners")
    public ResponseEntity<?> createBanner(@ModelAttribute BannerDto.BannerRequest request) {
        try {
            log.info("Creating banner with title: {}", request.getTitle());

            // Validate manually
            Map<String, String> errors = validateBannerRequest(request, true);
            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(errors);
            }

            BannerDto.BannerResponse response = bannerService.createBanner(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: ", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating banner: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi server: " + e.getMessage()));
        }
    }

    @PutMapping("/admin/banners/{id}")
    public ResponseEntity<?> updateBanner(
            @PathVariable Integer id,
            @ModelAttribute BannerDto.BannerRequest request
    ) {
        try {
            log.info("Updating banner {} with title: {}", id, request.getTitle());

            Map<String, String> errors = validateBannerRequest(request, false);
            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(errors);
            }

            BannerDto.BannerResponse response = bannerService.updateBanner(id, request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: ", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Not found error: ", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating banner: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi server: " + e.getMessage()));
        }
    }

    @DeleteMapping("/admin/banners/{id}")
    public ResponseEntity<?> deleteBanner(@PathVariable Integer id) {
        try {
            bannerService.deleteBanner(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Not found error: ", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting banner: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi server: " + e.getMessage()));
        }
    }

    @PatchMapping("/admin/banners/{id}/restore")
    public ResponseEntity<?> restoreBanner(@PathVariable Integer id) {
        try {
            bannerService.restoreBanner(id);
            return ResponseEntity.ok(Map.of("message", "Khôi phục thành công"));
        } catch (RuntimeException e) {
            log.error("Not found error: ", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error restoring banner: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi server: " + e.getMessage()));
        }
    }

    // ========== VALIDATION HELPERS ==========
    private Map<String, String> validateBannerRequest(BannerDto.BannerRequest request, boolean isCreate) {
        Map<String, String> errors = new HashMap<>();

        // Validate title
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            errors.put("title", "Tiêu đề banner không được để trống");
        } else if (request.getTitle().trim().length() < 3) {
            errors.put("title", "Tiêu đề phải có ít nhất 3 ký tự");
        }

        // Validate image file (only for create)
        if (isCreate && (request.getImageFile() == null || request.getImageFile().isEmpty())) {
            errors.put("image", "Vui lòng chọn ảnh banner");
        }

        // Validate image file size and type (if present)
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            if (request.getImageFile().getSize() > 10 * 1024 * 1024) {
                errors.put("image", "Kích thước ảnh không được vượt quá 10MB");
            }
            String contentType = request.getImageFile().getContentType();
            if (contentType == null || !(contentType.equals("image/jpeg") ||
                    contentType.equals("image/png") ||
                    contentType.equals("image/webp"))) {
                errors.put("image", "Chỉ hỗ trợ định dạng JPG, PNG, WebP");
            }
        }

        // Validate link type
        String linkType = request.getLinkType();
        if (linkType == null || linkType.trim().isEmpty()) {
            errors.put("linkType", "Vui lòng chọn loại link");
        } else if (!linkType.equals("CATEGORY") && !linkType.equals("PRODUCT") && !linkType.equals("URL")) {
            errors.put("linkType", "Loại link không hợp lệ");
        }

        // Validate category
        if ("CATEGORY".equals(linkType) && request.getCategoryId() == null) {
            errors.put("categoryId", "Vui lòng chọn danh mục");
        }

        // Validate product
        if ("PRODUCT".equals(linkType) && request.getProductId() == null) {
            errors.put("productId", "Vui lòng chọn sản phẩm");
        }

        // Validate custom URL
        if ("URL".equals(linkType) && (request.getCustomUrl() == null || request.getCustomUrl().trim().isEmpty())) {
            errors.put("customUrl", "Vui lòng nhập đường dẫn");
        }

        // Validate display order
        if (request.getDisplayOrder() != null && request.getDisplayOrder() < 0) {
            errors.put("displayOrder", "Thứ tự hiển thị phải lớn hơn hoặc bằng 0");
        }

        return errors;
    }
}