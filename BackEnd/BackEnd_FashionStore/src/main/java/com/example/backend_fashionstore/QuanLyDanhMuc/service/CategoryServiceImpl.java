package com.example.backend_fashionstore.QuanLyDanhMuc.service;

import com.example.backend_fashionstore.QuanLyDanhMuc.dto.request.CategoryRequestDTO;
import com.example.backend_fashionstore.QuanLyDanhMuc.dto.response.CategoryResponseDTO;
import com.example.backend_fashionstore.QuanLyDanhMuc.repository.CategoryRepository;
import com.example.backend_fashionstore.entity.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// Ghi đè file: src/main/java/com/example/backend_fashionstore/QuanLyDanhMuc/service/CategoryServiceImpl.java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    private CategoryResponseDTO convertToResponse(Category category) {
        return CategoryResponseDTO.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .categoryDescription(category.getCategoryDescription())
                .status(category.getStatus())
                .parentCategoryId(category.getParentCategoryId())
                .categoryIsLocked(Boolean.TRUE.equals(category.getIsLocked()))
                .build();
    }

    @Override
    public List<CategoryResponseDTO> getAll() {
        return categoryRepository.findAllActive()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryResponseDTO> getActiveCategories() {
        return categoryRepository.findActiveCategories()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponseDTO getById(Integer id) {
        Category category = categoryRepository.findByCategoryIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + id));
        return convertToResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO request) {
        if (categoryRepository.existsByCategoryNameAndDeletedAtIsNull(request.getCategoryName())) {
            throw new RuntimeException("Tên danh mục '" + request.getCategoryName() + "' đã tồn tại");
        }

        validateParent(request.getParentCategoryId(), null);

        String status = request.getStatus() != null ? request.getStatus() : "ACTIVE";
        if (!status.matches("ACTIVE|INACTIVE")) {
            status = "ACTIVE";
        }

        Category category = Category.builder()
                .categoryName(request.getCategoryName())
                .categoryDescription(request.getCategoryDescription())
                .status(status)
                .parentCategoryId(request.getParentCategoryId())
                .isLocked(false) // Không cho phép tạo mới danh mục đã bị khóa qua API
                .build();

        return convertToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponseDTO update(Integer id, CategoryRequestDTO request) {
        // ✅ Chỉ cho phép update khi chưa bị xóa
        Category category = categoryRepository.findByCategoryIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + id + " hoặc đã bị xóa. Vui lòng khôi phục trước khi sửa."));

        // (MỚI) Danh mục khóa cứng (Áo/Quần/Phụ kiện/Khuyến Mãi) không cho sửa
        if (Boolean.TRUE.equals(category.getIsLocked())) {
            throw new RuntimeException("Danh mục '" + category.getCategoryName() + "' là danh mục mặc định, không thể chỉnh sửa");
        }

        if (!category.getCategoryName().equals(request.getCategoryName())
                && categoryRepository.existsByCategoryNameAndDeletedAtIsNull(request.getCategoryName())) {
            throw new RuntimeException("Tên danh mục '" + request.getCategoryName() + "' đã tồn tại");
        }

        validateParent(request.getParentCategoryId(), id);

        category.setCategoryName(request.getCategoryName());
        category.setCategoryDescription(request.getCategoryDescription());
        category.setParentCategoryId(request.getParentCategoryId());

        if (request.getStatus() != null) {
            if (!request.getStatus().matches("ACTIVE|INACTIVE")) {
                throw new RuntimeException("Status chỉ được là ACTIVE hoặc INACTIVE");
            }
            category.setStatus(request.getStatus());
        }

        return convertToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        // (MỚI) Không cho xóa danh mục khóa cứng
        Category category = categoryRepository.findByCategoryIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + id));
        if (Boolean.TRUE.equals(category.getIsLocked())) {
            throw new RuntimeException("Danh mục '" + category.getCategoryName() + "' là danh mục mặc định, không thể xóa");
        }

        int deletedCount = categoryRepository.softDelete(id, LocalDateTime.now());
        if (deletedCount == 0) {
            throw new RuntimeException("Không thể xóa danh mục với ID: " + id);
        }
        log.info("Đã xóa mềm danh mục ID: {}", id);
    }

    @Override
    @Transactional
    public void restore(Integer id) {
        int restoredCount = categoryRepository.restore(id);
        if (restoredCount == 0) {
            throw new RuntimeException("Không thể khôi phục danh mục với ID: " + id);
        }
        log.info("Đã khôi phục danh mục ID: {}", id);
    }

    @Override
    public Page<CategoryResponseDTO> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("categoryName").ascending());
        return categoryRepository.searchByKeyword(keyword, pageable)
                .map(this::convertToResponse);
    }

    @Override
    public long count() {
        return categoryRepository.countByDeletedAtIsNull();
    }

    // Kiểm tra danh mục cha khi gán trực tiếp ở màn Quản lý danh mục.
    // Áp dụng đúng luật tối đa 2 cấp giống module Phân cấp danh mục.
    private void validateParent(Integer parentCategoryId, Integer selfId) {
        if (parentCategoryId == null) return;

        if (selfId != null && parentCategoryId.equals(selfId)) {
            throw new RuntimeException("Danh mục không thể là danh mục cha của chính nó");
        }

        Category parent = categoryRepository.findByCategoryIdAndDeletedAtIsNull(parentCategoryId)
                .orElseThrow(() -> new RuntimeException("Danh mục cha ID " + parentCategoryId + " không tồn tại"));

        if (parent.getParentCategoryId() != null) {
            throw new RuntimeException("Danh mục cha đã là danh mục con (cấp 2), không thể tạo thêm cấp 3");
        }
    }
}