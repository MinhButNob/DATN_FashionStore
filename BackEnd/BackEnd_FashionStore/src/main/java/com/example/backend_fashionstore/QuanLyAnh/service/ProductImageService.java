package com.example.backend_fashionstore.QuanLyAnh.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductDetailRepository productDetailRepository;
    private final Cloudinary cloudinary;

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

        // Xóa file trên Cloudinary
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

    // Helper: Lưu file ảnh lên Cloudinary
    private String saveImageFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được rỗng");
        }
        @SuppressWarnings("rawtypes")
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("folder", "product-images")
        );
        return uploadResult.get("secure_url").toString();
    }

    // Helper: Xóa file ảnh cũ trên Cloudinary
    private void deleteOldImageFile(String imagePath) {
        try {
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                String publicId = extractPublicId(imagePath);
                if (publicId != null && !publicId.trim().isEmpty()) {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                    log.info("Xóa ảnh thành công trên Cloudinary với public_id: {}", publicId);
                }
            }
        } catch (Exception e) {
            log.warn("Không thể xóa file ảnh cũ trên Cloudinary: {}", e.getMessage());
        }
    }

    // Helper: Trích xuất public_id từ URL Cloudinary
    private String extractPublicId(String imagePath) {
        if (imagePath == null || !imagePath.startsWith("http")) {
            return null;
        }
        int uploadIndex = imagePath.indexOf("/upload/");
        if (uploadIndex == -1) {
            return null;
        }
        String pathAfterUpload = imagePath.substring(uploadIndex + "/upload/".length());

        // Loại bỏ version prefix nếu có (ví dụ: v1612345678/)
        if (pathAfterUpload.matches("^v\\d+/.*")) {
            pathAfterUpload = pathAfterUpload.substring(pathAfterUpload.indexOf('/') + 1);
        }

        // Loại bỏ phần mở rộng file (ví dụ: .jpg, .png)
        int lastDotIndex = pathAfterUpload.lastIndexOf('.');
        if (lastDotIndex != -1) {
            pathAfterUpload = pathAfterUpload.substring(0, lastDotIndex);
        }
        return pathAfterUpload;
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