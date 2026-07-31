package com.example.backend_fashionstore.QuanLyAnh.controller;

import com.example.backend_fashionstore.QuanLyAnh.dto.ProductImageDto;
import com.example.backend_fashionstore.QuanLyAnh.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-images")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService productImageService;

    @GetMapping
    public ResponseEntity<Page<ProductImageDto.ProductImageResponse>> getImages(
            @RequestParam(required = false) Integer productDetailId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                productImageService.getImages(productDetailId, keyword, sortBy, sortDirection, page, size)
        );
    }

    @GetMapping("/total-pages")
    public ResponseEntity<Integer> getTotalPages(
            @RequestParam(required = false) Integer productDetailId,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(productImageService.getTotalPages(productDetailId, size));
    }

    @PostMapping
    public ResponseEntity<?> createImage(@ModelAttribute ProductImageDto.ProductImageRequest request) {
        try {
            ProductImageDto.ProductImageResponse response = productImageService.createImage(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductImageDto.ProductImageResponse> updateImage(
            @PathVariable Integer id,
            @ModelAttribute ProductImageDto.ProductImageRequest request
    ) {
        try {
            ProductImageDto.ProductImageResponse response = productImageService.updateImage(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Integer id) {
        try {
            productImageService.deleteImage(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ THÊM API RESTORE
    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restoreImage(@PathVariable Integer id) {
        try {
            productImageService.restoreImage(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }


}