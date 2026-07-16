package com.example.backend_fashionstore.PhanCapDanhMuc.service;

import com.example.backend_fashionstore.PhanCapDanhMuc.dto.request.CategoryHierarchyRequestDTO;
import com.example.backend_fashionstore.PhanCapDanhMuc.dto.response.CategoryHierarchyResponseDTO;
import com.example.backend_fashionstore.PhanCapDanhMuc.dto.response.CategoryTreeResponseDTO;

import java.util.List;

public interface CategoryHierarchyService {
    List<CategoryTreeResponseDTO> getFullTree();
    List<CategoryTreeResponseDTO> getActiveTree();
    List<CategoryHierarchyResponseDTO> getParentOptions();
    List<CategoryHierarchyResponseDTO> getChildren(Integer parentId);
    CategoryHierarchyResponseDTO create(CategoryHierarchyRequestDTO request);
    CategoryHierarchyResponseDTO update(Integer id, CategoryHierarchyRequestDTO request);
    void delete(Integer id);
    void restore(Integer id);
}