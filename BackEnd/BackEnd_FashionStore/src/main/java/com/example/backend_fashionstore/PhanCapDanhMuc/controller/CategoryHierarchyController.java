package com.example.backend_fashionstore.PhanCapDanhMuc.controller;

import com.example.backend_fashionstore.PhanCapDanhMuc.dto.request.CategoryHierarchyRequestDTO;
import com.example.backend_fashionstore.PhanCapDanhMuc.dto.response.CategoryHierarchyResponseDTO;
import com.example.backend_fashionstore.PhanCapDanhMuc.dto.response.CategoryTreeResponseDTO;
import com.example.backend_fashionstore.PhanCapDanhMuc.service.CategoryHierarchyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Module PHÂN CẤP DANH MỤC (2 cấp: cha - con).
 * Base path: /api/category-hierarchy
 * Hoàn toàn tách biệt với /api/categories (module QuanLyDanhMuc cũ không đổi gì).
 */
@RestController
@RequestMapping("/api/category-hierarchy")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CategoryHierarchyController {

    private final CategoryHierarchyService categoryHierarchyService;

    // Cây đầy đủ (ACTIVE + INACTIVE) - dùng cho trang quản trị
    @GetMapping("/tree")
    public ResponseEntity<List<CategoryTreeResponseDTO>> getFullTree() {
        return ResponseEntity.ok(categoryHierarchyService.getFullTree());
    }

    // Cây chỉ ACTIVE - dùng cho menu ngoài trang khách hàng
    @GetMapping("/tree/active")
    public ResponseEntity<List<CategoryTreeResponseDTO>> getActiveTree() {
        return ResponseEntity.ok(categoryHierarchyService.getActiveTree());
    }

    @GetMapping("/parents")
    public ResponseEntity<List<CategoryHierarchyResponseDTO>> getParentOptions() {
        return ResponseEntity.ok(categoryHierarchyService.getParentOptions());
    }

    @GetMapping("/children/{parentId}")
    public ResponseEntity<List<CategoryHierarchyResponseDTO>> getChildren(@PathVariable Integer parentId) {
        return ResponseEntity.ok(categoryHierarchyService.getChildren(parentId));
    }

    @PostMapping
    public ResponseEntity<CategoryHierarchyResponseDTO> create(@Valid @RequestBody CategoryHierarchyRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryHierarchyService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryHierarchyResponseDTO> update(@PathVariable Integer id,
                                                               @Valid @RequestBody CategoryHierarchyRequestDTO request) {
        return ResponseEntity.ok(categoryHierarchyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        categoryHierarchyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Integer id) {
        categoryHierarchyService.restore(id);
        return ResponseEntity.ok().build();
    }
}