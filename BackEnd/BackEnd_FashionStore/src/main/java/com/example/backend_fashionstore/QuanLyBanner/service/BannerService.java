package com.example.backend_fashionstore.QuanLyBanner.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.backend_fashionstore.QuanLyBanner.dto.BannerDto;
import com.example.backend_fashionstore.QuanLyBanner.repository.BannerRepository;
import com.example.backend_fashionstore.QuanLySanPham.repository.Product.ProductRepository;
import com.example.backend_fashionstore.QuanLyDanhMuc.repository.CategoryRepository;
import com.example.backend_fashionstore.entity.Banner;
import com.example.backend_fashionstore.entity.Category;
import com.example.backend_fashionstore.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BannerService {

    private final BannerRepository bannerRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final Cloudinary cloudinary;

    public List<BannerDto.BannerResponse> getActiveBanners() {
        try {
            return bannerRepository.findByStatusOrderByDisplayOrderAsc("ACTIVE")
                    .stream()
                    .map(BannerDto::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching active banners: ", e);
            throw new RuntimeException("Không thể tải banner active: " + e.getMessage());
        }
    }

    public Page<BannerDto.BannerResponse> getBanners(String keyword, String sortBy, String sortDirection, int page, int size) {
        try {
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
            String field = mapSortField(sortBy);
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, field));

            Page<Banner> bannerPage = bannerRepository.searchBanners(keyword, pageable);
            List<BannerDto.BannerResponse> responses = bannerPage.getContent()
                    .stream()
                    .map(BannerDto::toResponse)
                    .collect(Collectors.toList());

            return new PageImpl<>(responses, pageable, bannerPage.getTotalElements());
        } catch (Exception e) {
            log.error("Error fetching banners: ", e);
            throw new RuntimeException("Không thể tải danh sách banner: " + e.getMessage());
        }
    }

    public BannerDto.BannerResponse createBanner(BannerDto.BannerRequest request) {
        try {
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Tiêu đề banner không được để trống");
            }

            if (request.getImageFile() == null || request.getImageFile().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn ảnh banner");
            }

            String imagePath = saveImage(request.getImageFile());

            Banner banner = Banner.builder()
                    .title(request.getTitle().trim())
                    .subtitle(request.getSubtitle() != null ? request.getSubtitle().trim() : null)
                    .imagePath(imagePath)
                    .linkType(request.getLinkType())
                    .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                    .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                    .build();

            setLinkTarget(banner, request);
            Banner savedBanner = bannerRepository.save(banner);
            log.info("Created banner with id: {}", savedBanner.getBannerId());

            return BannerDto.toResponse(savedBanner);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating banner: ", e);
            throw new RuntimeException("Không thể tạo banner: " + e.getMessage());
        }
    }

    public BannerDto.BannerResponse updateBanner(Integer id, BannerDto.BannerRequest request) {
        try {
            Banner banner = bannerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy banner với id: " + id));

            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Tiêu đề banner không được để trống");
            }

            banner.setTitle(request.getTitle().trim());
            banner.setSubtitle(request.getSubtitle() != null ? request.getSubtitle().trim() : null);
            banner.setLinkType(request.getLinkType());
            banner.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : banner.getDisplayOrder());

            if (request.getStatus() != null) {
                banner.setStatus(request.getStatus());
            }

            if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
                deleteOldImage(banner.getImagePath());
                String newImagePath = saveImage(request.getImageFile());
                banner.setImagePath(newImagePath);
            }

            setLinkTarget(banner, request);
            Banner updatedBanner = bannerRepository.save(banner);
            log.info("Updated banner with id: {}", updatedBanner.getBannerId());

            return BannerDto.toResponse(updatedBanner);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating banner: ", e);
            throw new RuntimeException("Không thể cập nhật banner: " + e.getMessage());
        }
    }

    public void deleteBanner(Integer id) {
        try {
            Banner banner = bannerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy banner với id: " + id));
            banner.setStatus("DELETED");
            banner.setDeletedAt(LocalDateTime.now());
            bannerRepository.save(banner);
            log.info("Deleted banner with id: {}", id);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting banner: ", e);
            throw new RuntimeException("Không thể xóa banner: " + e.getMessage());
        }
    }

    public void restoreBanner(Integer id) {
        try {
            Banner banner = bannerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy banner với id: " + id));
            banner.setStatus("INACTIVE");
            banner.setDeletedAt(null);
            bannerRepository.save(banner);
            log.info("Restored banner with id: {}", id);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error restoring banner: ", e);
            throw new RuntimeException("Không thể khôi phục banner: " + e.getMessage());
        }
    }

    // ========== PRIVATE HELPERS ==========

    private String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được trống");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Kích thước ảnh không được vượt quá 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/webp"))) {
            throw new IllegalArgumentException("Chỉ hỗ trợ định dạng JPG, PNG, WebP");
        }

        @SuppressWarnings("rawtypes")
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("folder", "banners")
        );
        log.info("Uploaded banner image to Cloudinary: {}", uploadResult.get("secure_url"));
        return uploadResult.get("secure_url").toString();
    }

    private void deleteOldImage(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) return;
        try {
            String publicId = extractPublicId(imagePath);
            if (publicId != null && !publicId.trim().isEmpty()) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Deleted banner image on Cloudinary with public_id: {}", publicId);
            }
        } catch (Exception e) {
            log.warn("Không thể xóa ảnh banner cũ trên Cloudinary: {}", imagePath);
        }
    }

    private String extractPublicId(String imagePath) {
        if (imagePath == null || !imagePath.startsWith("http")) {
            return null;
        }
        int uploadIndex = imagePath.indexOf("/upload/");
        if (uploadIndex == -1) {
            return null;
        }
        String pathAfterUpload = imagePath.substring(uploadIndex + "/upload/".length());

        if (pathAfterUpload.matches("^v\\d+/.*")) {
            pathAfterUpload = pathAfterUpload.substring(pathAfterUpload.indexOf('/') + 1);
        }

        int lastDotIndex = pathAfterUpload.lastIndexOf('.');
        if (lastDotIndex != -1) {
            pathAfterUpload = pathAfterUpload.substring(0, lastDotIndex);
        }
        return pathAfterUpload;
    }

    private void setLinkTarget(Banner banner, BannerDto.BannerRequest request) {
        banner.setCategory(null);
        banner.setProduct(null);
        banner.setCustomUrl(null);

        String linkType = request.getLinkType();
        if (linkType == null) return;

        switch (linkType) {
            case "CATEGORY":
                if (request.getCategoryId() != null) {
                    Category cat = categoryRepository.findById(request.getCategoryId())
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục với id: " + request.getCategoryId()));
                    banner.setCategory(cat);
                }
                break;
            case "PRODUCT":
                if (request.getProductId() != null) {
                    Product prod = productRepository.findById(request.getProductId())
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với id: " + request.getProductId()));
                    banner.setProduct(prod);
                }
                break;
            case "URL":
                if (request.getCustomUrl() != null && !request.getCustomUrl().trim().isEmpty()) {
                    banner.setCustomUrl(request.getCustomUrl().trim());
                } else {
                    throw new IllegalArgumentException("Vui lòng nhập đường dẫn");
                }
                break;
            default:
                throw new IllegalArgumentException("Loại link không hợp lệ: " + linkType);
        }
    }

    private String mapSortField(String sortBy) {
        if (sortBy == null) return "bannerId";
        return switch (sortBy) {
            case "title" -> "title";
            case "displayOrder" -> "displayOrder";
            case "status" -> "status";
            case "createdAt" -> "createdAt";
            default -> "bannerId";
        };
    }
}