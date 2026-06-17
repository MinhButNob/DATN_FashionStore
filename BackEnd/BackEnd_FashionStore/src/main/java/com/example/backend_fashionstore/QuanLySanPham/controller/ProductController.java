package com.example.backend_fashionstore.QuanLySanPham.controller;

import com.example.backend_fashionstore.QuanLySanPham.dto.request.ProductCreateRequest;
import com.example.backend_fashionstore.QuanLySanPham.dto.request.ProductFilterRequest;
import com.example.backend_fashionstore.QuanLySanPham.dto.response.ApiResponse;
import com.example.backend_fashionstore.QuanLySanPham.dto.response.PageResponse;
import com.example.backend_fashionstore.QuanLySanPham.dto.response.ProductResponse;
import com.example.backend_fashionstore.QuanLySanPham.dto.response.ProductStatsResponse;
import com.example.backend_fashionstore.QuanLySanPham.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(ProductFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getProducts(filter)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getProductById(id)));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<ProductStatsResponse>> getStatistics() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getStatistics()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(productService.createProduct(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Integer id,
            @Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(productService.updateProduct(id, request)));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<ProductResponse>> toggleStatus(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.toggleStatus(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ✅ THÊM API RESTORE
    @PatchMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<ProductResponse>> restoreProduct(@PathVariable Integer id) {
        ProductResponse product = productService.restoreProduct(id);
        return ResponseEntity.ok(ApiResponse.ok(product));
    }
}