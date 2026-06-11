package com.example.backend_fashionstore.QuanLyAnh.service;

import com.example.backend_fashionstore.QuanLyAnh.dto.ProductImageDto;
import com.example.backend_fashionstore.QuanLyAnh.repository.ProductImageRepository;
import com.example.backend_fashionstore.entity.ProductDetail;
import com.example.backend_fashionstore.entity.ProductImage;
import com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductDetailRepository productDetailRepository;
    private final String UPLOAD_DIR = "uploads/product-images/";

    // Lấy danh sách có phân trang và lọc
    public Page<ProductImageDto.ProductImageResponse> getImages(
            Integer productDetailId,
            String keyword,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortField = getSortField(sortBy);
        Sort sort = Sort.by(direction, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductImage> imagePage;

        if (productDetailId != null) {
            imagePage = productImageRepository.findByProductDetail_ProductDetailId(productDetailId, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            imagePage = productImageRepository.findByProductImagePathContaining(keyword, pageable);
        } else {
            imagePage = productImageRepository.findAll(pageable);
        }

        List<ProductImageDto.ProductImageResponse> responses = imagePage.getContent()
                .stream()
                .map(ProductImageDto::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, imagePage.getTotalElements());
    }

    // Lấy tổng số trang
    public int getTotalPages(Integer productDetailId, int size) {
        long totalItems;
        if (productDetailId != null) {
            totalItems = productImageRepository.countByProductDetail_ProductDetailId(productDetailId);
        } else {
            totalItems = productImageRepository.count();
        }
        return (int) Math.ceil((double) totalItems / size);
    }

    // Tạo mới ảnh
    public ProductImageDto.ProductImageResponse createImage(ProductImageDto.ProductImageRequest request) throws IOException {
        if (request.getImageFile() == null || request.getImageFile().isEmpty()) {
            throw new RuntimeException("Vui lòng chọn file ảnh");
        }

        ProductDetail productDetail = productDetailRepository.findById(request.getProductDetailId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Product Detail với ID: " + request.getProductDetailId()));

        String imagePath = saveImageFile(request.getImageFile());

        Integer displayOrder = request.getDisplayOrder();
        if (displayOrder == null) {
            displayOrder = productImageRepository.getMaxDisplayOrderByProductDetailId(request.getProductDetailId()) + 1;
        }

        Boolean isMain = request.getIsMain() != null && request.getIsMain();
        if (isMain) {
            removeMainImageStatus(request.getProductDetailId());
        }

        ProductImage image = ProductImage.builder()
                .productDetail(productDetail)
                .productImagePath(imagePath)
                .productImageType(request.getProductImageType() != null ? request.getProductImageType() : "GALLERY")
                .displayOrder(displayOrder)
                .isMain(isMain)
                .status("ACTIVE")
                .build();

        ProductImage savedImage = productImageRepository.save(image);
        log.info("Tạo ảnh thành công với ID: {}", savedImage.getProductImageId());

        return ProductImageDto.toResponse(savedImage);
    }

    // Cập nhật ảnh
    public ProductImageDto.ProductImageResponse updateImage(Integer id, ProductImageDto.ProductImageRequest request) throws IOException {
        ProductImage image = productImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh với ID: " + id));

        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            deleteOldImageFile(image.getProductImagePath());
            String newPath = saveImageFile(request.getImageFile());
            image.setProductImagePath(newPath);
        }

        if (request.getProductImageType() != null) {
            image.setProductImageType(request.getProductImageType());
        }

        if (request.getDisplayOrder() != null) {
            image.setDisplayOrder(request.getDisplayOrder());
        }

        if (request.getIsMain() != null && request.getIsMain()) {
            removeMainImageStatus(image.getProductDetail().getProductDetailId());
            image.setIsMain(true);
        } else if (request.getIsMain() != null && !request.getIsMain()) {
            image.setIsMain(false);
        }

        ProductImage updatedImage = productImageRepository.save(image);
        log.info("Cập nhật ảnh thành công với ID: {}", id);

        return ProductImageDto.toResponse(updatedImage);
    }

    // Xóa mềm ảnh
    public void deleteImage(Integer id) {
        ProductImage image = productImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh với ID: " + id));

        Integer productDetailId = image.getProductDetail().getProductDetailId();
        Boolean wasMain = image.getIsMain();

        int deletedCount = productImageRepository.softDelete(id);
        if (deletedCount == 0) {
            throw new RuntimeException("Không thể xóa ảnh với ID: " + id);
        }

        // Xóa file vật lý
        deleteOldImageFile(image.getProductImagePath());

        // Nếu xóa ảnh chính, set ảnh đầu tiên làm main
        if (wasMain) {
            List<ProductImage> remainingImages = productImageRepository
                    .findAllByProductDetailIdOrderByDisplayOrder(productDetailId);
            if (!remainingImages.isEmpty()) {
                ProductImage newMain = remainingImages.get(0);
                newMain.setIsMain(true);
                productImageRepository.save(newMain);
                log.info("Đặt ảnh mới làm ảnh chính cho product_detail_id: {}", productDetailId);
            }
        }

        log.info("Xóa mềm ảnh thành công với ID: {}", id);
    }

    // ✅ THÊM: Khôi phục ảnh đã xóa mềm
    public void restoreImage(Integer id) {
        int restoredCount = productImageRepository.restore(id);
        if (restoredCount == 0) {
            throw new RuntimeException("Không thể khôi phục ảnh với ID: " + id);
        }
        log.info("Khôi phục ảnh thành công với ID: {}", id);
    }

    // Helper: Xóa trạng thái main của tất cả ảnh trong product_detail
    private void removeMainImageStatus(Integer productDetailId) {
        List<ProductImage> images = productImageRepository.findByProductDetailId(productDetailId);
        for (ProductImage img : images) {
            if (img.getIsMain()) {
                img.setIsMain(false);
                productImageRepository.save(img);
            }
        }
    }

    // Helper: Lưu file ảnh
    private String saveImageFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString() + extension;

        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath);

        return UPLOAD_DIR + newFilename;
    }

    // Helper: Xóa file cũ
    private void deleteOldImageFile(String imagePath) {
        try {
            if (imagePath != null) {
                Path path = Paths.get(imagePath);
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.warn("Không thể xóa file ảnh cũ: {}", e.getMessage());
        }
    }

    // Helper: Lấy field sort
    private String getSortField(String sortBy) {
        if (sortBy == null) return "displayOrder";
        return switch (sortBy) {
            case "id" -> "productImageId";
            case "displayOrder" -> "displayOrder";
            case "newest" -> "createdAt";
            default -> "displayOrder";
        };
    }
}