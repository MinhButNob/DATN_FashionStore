package com.example.backend_fashionstore.quanLyKhuyenMai.controller;

import com.example.backend_fashionstore.quanLyKhuyenMai.dto.*;
import com.example.backend_fashionstore.quanLyKhuyenMai.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public ResponseEntity<Page<PromotionDTO>> getAllPromotions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "promotionId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PromotionDTO> promotions = promotionService.filterAndSearchPromotions(status, keyword, pageable);
        return ResponseEntity.ok(promotions);
    }

    // Lấy danh sách đã xóa
    @GetMapping("/deleted")
    public ResponseEntity<Page<PromotionDTO>> getDeletedPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(promotionService.getDeletedPromotions(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionDTO> getPromotionById(@PathVariable Integer id) {
        return ResponseEntity.ok(promotionService.getPromotionById(id));
    }

    @PostMapping
    public ResponseEntity<PromotionDTO> createPromotion(@Valid @RequestBody PromotionCreateDTO createDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(promotionService.createPromotion(createDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionDTO> updatePromotion(
            @PathVariable Integer id,
            @Valid @RequestBody PromotionUpdateDTO updateDTO
    ) {
        return ResponseEntity.ok(promotionService.updatePromotion(id, updateDTO));
    }

    // Xóa mềm (ẩn)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeletePromotion(@PathVariable Integer id) {
        promotionService.softDeletePromotion(id);
        return ResponseEntity.noContent().build();
    }

    // Khôi phục
    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restorePromotion(@PathVariable Integer id) {
        promotionService.restorePromotion(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Void> togglePromotionStatus(@PathVariable Integer id) {
        promotionService.togglePromotionStatus(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/statistics")
    public ResponseEntity<PromotionStatisticsDTO> getStatistics() {
        return ResponseEntity.ok(promotionService.getStatistics());
    }
}