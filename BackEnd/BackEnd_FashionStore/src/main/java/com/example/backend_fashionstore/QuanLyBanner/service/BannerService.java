package com.example.backend_fashionstore.QuanLyBanner.service;

import com.example.backend_fashionstore.QuanLyBanner.dto.BannerDto;
import com.example.backend_fashionstore.QuanLyBanner.repository.BannerRepository;
import com.example.backend_fashionstore.QuanLySanPham.repository.Product.ProductRepository;
import com.example.backend_fashionstore.QuanLyDanhMuc.repository.CategoryRepository;
import com.example.backend_fashionstore.entity.Banner;
import com.example.backend_fashionstore.entity.Category;
import com.example.backend_fashionstore.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service  // ✅ QUAN TRỌNG: Phải có @Service
@RequiredArgsConstructor
@Transactional
public class BannerService {

    private final BannerRepository bannerRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    private static final String UPLOAD_DIR = "uploads/banners/";

    // ========== CLIENT ==========
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

    // ========== ADMIN ==========
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

    // ========== CREATE BANNER ==========
    public BannerDto.BannerResponse createBanner(BannerDto.BannerRequest request) {
        try {
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Tiêu đề banner không được để trống");
            }

            if (request.getImageFile() == null || request.getImageFile().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn ảnh banner");
            }

            Integer displayOrder = request.getDisplayOrder();

            if (displayOrder == null) {
                Integer maxOrder = bannerRepository.findMaxDisplayOrder();
                displayOrder = (maxOrder == null) ? 1 : maxOrder + 1;
                log.info("Auto-assigned displayOrder: {}", displayOrder);
            } else {
                if (displayOrder < 0) {
                    throw new IllegalArgumentException("Thứ tự hiển thị phải lớn hơn hoặc bằng 0");
                }
                boolean exists = bannerRepository.existsByDisplayOrderAndNotDeleted(displayOrder);
                if (exists) {
                    throw new IllegalArgumentException(
                            "Thứ tự " + displayOrder + " đã được sử dụng. Vui lòng chọn thứ tự khác."
                    );
                }
            }

            String imagePath = saveImage(request.getImageFile());

            Banner banner = Banner.builder()
                    .title(request.getTitle().trim())
                    .subtitle(request.getSubtitle() != null ? request.getSubtitle().trim() : null)
                    .imagePath(imagePath)
                    .linkType(request.getLinkType())
                    .displayOrder(displayOrder)
                    .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                    .build();

            setLinkTarget(banner, request);
            Banner savedBanner = bannerRepository.save(banner);
            log.info("✅ Created banner with id: {}, order: {}", savedBanner.getBannerId(), savedBanner.getDisplayOrder());

            return BannerDto.toResponse(savedBanner);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating banner: ", e);
            throw new RuntimeException("Không thể tạo banner: " + e.getMessage());
        }
    }

    // ========== UPDATE BANNER ==========
    public BannerDto.BannerResponse updateBanner(Integer id, BannerDto.BannerRequest request) {
        try {
            Banner banner = bannerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy banner với id: " + id));

            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Tiêu đề banner không được để trống");
            }

            Integer displayOrder = request.getDisplayOrder();
            if (displayOrder != null) {
                if (displayOrder < 0) {
                    throw new IllegalArgumentException("Thứ tự hiển thị phải lớn hơn hoặc bằng 0");
                }
                boolean exists = bannerRepository.existsByDisplayOrderAndNotDeletedAndIdNot(displayOrder, id);
                if (exists) {
                    throw new IllegalArgumentException(
                            "Thứ tự " + displayOrder + " đã được sử dụng bởi banner khác. Vui lòng chọn thứ tự khác."
                    );
                }
                banner.setDisplayOrder(displayOrder);
            }

            banner.setTitle(request.getTitle().trim());
            banner.setSubtitle(request.getSubtitle() != null ? request.getSubtitle().trim() : null);
            banner.setLinkType(request.getLinkType());

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
            log.info("✅ Updated banner with id: {}, order: {}", updatedBanner.getBannerId(), updatedBanner.getDisplayOrder());

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

    // ========== DELETE BANNER ==========
    public void deleteBanner(Integer id) {
        try {
            Banner banner = bannerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy banner với id: " + id));

            Integer deletedOrder = banner.getDisplayOrder();

            banner.setStatus("DELETED");
            banner.setDeletedAt(LocalDateTime.now());
            bannerRepository.save(banner);

            if (deletedOrder != null) {
                bannerRepository.decrementDisplayOrderAfter(deletedOrder);
                log.info("✅ Re-ordered banners after deleting order: {}", deletedOrder);
            }

            log.info("✅ Deleted banner with id: {}", id);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting banner: ", e);
            throw new RuntimeException("Không thể xóa banner: " + e.getMessage());
        }
    }

    // ========== RESTORE BANNER ==========
    public void restoreBanner(Integer id) {
        try {
            Banner banner = bannerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy banner với id: " + id));
            banner.setStatus("INACTIVE");
            banner.setDeletedAt(null);
            bannerRepository.save(banner);
            log.info("✅ Restored banner with id: {}", id);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error restoring banner: ", e);
            throw new RuntimeException("Không thể khôi phục banner: " + e.getMessage());
        }
    }

    // ✅ ĐÃ SỬA - KHÔNG BỊ NULL
    public Map<String, Object> checkOrderExists(Integer displayOrder, Integer excludeId) {
        Map<String, Object> result = new HashMap<>();

        if (displayOrder == null) {
            result.put("exists", false);
            result.put("order", null);
            result.put("excludeId", excludeId != null ? excludeId : 0);
            result.put("message", "Vui lòng nhập thứ tự hiển thị");
            return result;
        }

        boolean exists;
        if (excludeId != null) {
            exists = bannerRepository.existsByDisplayOrderAndNotDeletedAndIdNot(displayOrder, excludeId);
        } else {
            exists = bannerRepository.existsByDisplayOrderAndNotDeleted(displayOrder);
        }

        result.put("exists", exists);
        result.put("order", displayOrder);
        result.put("excludeId", excludeId != null ? excludeId : 0);
        result.put("message", exists ? "Thứ tự này đã được sử dụng" : "Thứ tự hợp lệ");

        return result;
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

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String newFilename = UUID.randomUUID() + ".jpg";
        Path filePath = uploadPath.resolve(newFilename);

        try {
            Thumbnails.of(file.getInputStream())
                    .size(1920, 600)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(0.92)
                    .toFile(filePath.toFile());

            log.info("Saved image: {}", newFilename);
            return UPLOAD_DIR + newFilename;

        } catch (IOException e) {
            log.error("Error saving image: ", e);
            throw new IOException("Không thể lưu ảnh: " + e.getMessage());
        }
    }

    private void deleteOldImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return;
        try {
            Path path = Paths.get(imagePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("Deleted old image: {}", imagePath);
            }
        } catch (IOException e) {
            log.warn("Không thể xóa ảnh cũ: {}", imagePath);
        }
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