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

        String status = request.getStatus() != null ? request.getStatus() : "ACTIVE";
        if (!status.matches("ACTIVE|INACTIVE")) {
            status = "ACTIVE";
        }

        Category category = Category.builder()
                .categoryName(request.getCategoryName())
                .categoryDescription(request.getCategoryDescription())
                .status(status)
                .build();

        return convertToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponseDTO update(Integer id, CategoryRequestDTO request) {
        // ✅ Chỉ cho phép update khi chưa bị xóa
        Category category = categoryRepository.findByCategoryIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + id + " hoặc đã bị xóa. Vui lòng khôi phục trước khi sửa."));

        if (!category.getCategoryName().equals(request.getCategoryName())
                && categoryRepository.existsByCategoryNameAndDeletedAtIsNull(request.getCategoryName())) {
            throw new RuntimeException("Tên danh mục '" + request.getCategoryName() + "' đã tồn tại");
        }

        category.setCategoryName(request.getCategoryName());
        category.setCategoryDescription(request.getCategoryDescription());

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
}