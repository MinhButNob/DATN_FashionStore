package com.example.backend_fashionstore.PhanCapDanhMuc.service;

import com.example.backend_fashionstore.PhanCapDanhMuc.dto.request.CategoryHierarchyRequestDTO;
import com.example.backend_fashionstore.PhanCapDanhMuc.dto.response.CategoryHierarchyResponseDTO;
import com.example.backend_fashionstore.PhanCapDanhMuc.dto.response.CategoryTreeResponseDTO;
import com.example.backend_fashionstore.PhanCapDanhMuc.repository.CategoryHierarchyRepository;
import com.example.backend_fashionstore.entity.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// Ghi đè file: src/main/java/com/example/backend_fashionstore/PhanCapDanhMuc/service/CategoryHierarchyServiceImpl.java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryHierarchyServiceImpl implements CategoryHierarchyService {

    private final CategoryHierarchyRepository repository;

    private CategoryHierarchyResponseDTO convertToResponse(Category c) {
        return CategoryHierarchyResponseDTO.builder()
                .categoryId(c.getCategoryId())
                .categoryName(c.getCategoryName())
                .categoryDescription(c.getCategoryDescription())
                .status(c.getStatus())
                .parentCategoryId(c.getParentCategoryId())
                .categoryIsLocked(Boolean.TRUE.equals(c.getIsLocked()))
                .build();
    }

    // ============ ĐỌC DỮ LIỆU ============

    @Override
    public List<CategoryTreeResponseDTO> getFullTree() {
        return repository.findParentCategories().stream()
                .map(parent -> toTreeNode(parent, repository.findChildren(parent.getCategoryId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryTreeResponseDTO> getActiveTree() {
        return repository.findActiveParentCategories().stream()
                .map(parent -> toTreeNode(parent, repository.findActiveChildren(parent.getCategoryId())))
                .collect(Collectors.toList());
    }

    private CategoryTreeResponseDTO toTreeNode(Category parent, List<Category> children) {
        return CategoryTreeResponseDTO.builder()
                .categoryId(parent.getCategoryId())
                .categoryName(parent.getCategoryName())
                .categoryDescription(parent.getCategoryDescription())
                .status(parent.getStatus())
                .categoryIsLocked(Boolean.TRUE.equals(parent.getIsLocked()))
                .children(children.stream().map(this::convertToResponse).collect(Collectors.toList()))
                .build();
    }

    @Override
    public List<CategoryHierarchyResponseDTO> getParentOptions() {
        return repository.findParentCategories().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryHierarchyResponseDTO> getChildren(Integer parentId) {
        return repository.findChildren(parentId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ============ GHI DỮ LIỆU ============

    @Override
    @Transactional
    public CategoryHierarchyResponseDTO create(CategoryHierarchyRequestDTO request) {
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

        return convertToResponse(repository.save(category));
    }

    @Override
    @Transactional
    public CategoryHierarchyResponseDTO update(Integer id, CategoryHierarchyRequestDTO request) {
        Category category = repository.findByCategoryIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy danh mục với ID: " + id + " hoặc đã bị xóa. Vui lòng khôi phục trước khi sửa."));

        // (MỚI) Danh mục khóa cứng (Áo/Quần/Phụ kiện/Khuyến Mãi) không cho sửa
        if (Boolean.TRUE.equals(category.getIsLocked())) {
            throw new RuntimeException("Danh mục '" + category.getCategoryName() + "' là danh mục mặc định, không thể chỉnh sửa");
        }

        validateParent(request.getParentCategoryId(), id);

        // Luật tối đa 2 cấp (thay cho trigger): danh mục đang có con thì
        // không được biến nó thành con của danh mục khác
        if (request.getParentCategoryId() != null && repository.existsActiveChildren(id)) {
            throw new RuntimeException(
                    "Danh mục này đang có danh mục con, không thể chuyển thành danh mục con của danh mục khác");
        }

        category.setCategoryName(request.getCategoryName());
        category.setCategoryDescription(request.getCategoryDescription());
        category.setParentCategoryId(request.getParentCategoryId());

        if (request.getStatus() != null) {
            if (!request.getStatus().matches("ACTIVE|INACTIVE")) {
                throw new RuntimeException("Status chỉ được là ACTIVE hoặc INACTIVE");
            }
            category.setStatus(request.getStatus());
        }

        return convertToResponse(repository.save(category));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        // (MỚI) Không cho xóa danh mục khóa cứng
        Category category = repository.findByCategoryIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + id));
        if (Boolean.TRUE.equals(category.getIsLocked())) {
            throw new RuntimeException("Danh mục '" + category.getCategoryName() + "' là danh mục mặc định, không thể xóa");
        }

        if (repository.existsActiveChildren(id)) {
            throw new RuntimeException("Không thể xóa danh mục đang có danh mục con, hãy xóa danh mục con trước");
        }

        int deletedCount = repository.softDelete(id, LocalDateTime.now());
        if (deletedCount == 0) {
            throw new RuntimeException("Không thể xóa danh mục với ID: " + id);
        }
        log.info("Đã xóa mềm danh mục (phân cấp) ID: {}", id);
    }

    @Override
    @Transactional
    public void restore(Integer id) {
        int restoredCount = repository.restore(id);
        if (restoredCount == 0) {
            throw new RuntimeException("Không thể khôi phục danh mục với ID: " + id);
        }
        log.info("Đã khôi phục danh mục (phân cấp) ID: {}", id);
    }

    // ============ VALIDATION DÙNG CHUNG (thay cho trigger) ============

    private void validateParent(Integer parentCategoryId, Integer selfId) {
        if (parentCategoryId == null) return;

        if (selfId != null && parentCategoryId.equals(selfId)) {
            throw new RuntimeException("Danh mục không thể là danh mục cha của chính nó");
        }

        Category parent = repository.findByCategoryIdAndDeletedAtIsNull(parentCategoryId)
                .orElseThrow(() -> new RuntimeException("Danh mục cha ID " + parentCategoryId + " không tồn tại"));

        if (parent.getParentCategoryId() != null) {
            throw new RuntimeException("Danh mục cha đã là danh mục con (cấp 2), không thể tạo thêm cấp 3");
        }
    }
}