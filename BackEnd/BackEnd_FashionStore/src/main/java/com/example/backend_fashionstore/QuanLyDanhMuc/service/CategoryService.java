package com.example.backend_fashionstore.QuanLyDanhMuc.service;

import com.example.backend_fashionstore.QuanLyDanhMuc.dto.request.CategoryRequestDTO;
import com.example.backend_fashionstore.QuanLyDanhMuc.dto.response.CategoryResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CategoryService {
    List<CategoryResponseDTO> getAll();
    List<CategoryResponseDTO> getActiveCategories();
    CategoryResponseDTO getById(Integer id);
    CategoryResponseDTO create(CategoryRequestDTO request);
    CategoryResponseDTO update(Integer id, CategoryRequestDTO request);
    void delete(Integer id);
    void restore(Integer id);
    Page<CategoryResponseDTO> search(String keyword, int page, int size);
    long count();
}