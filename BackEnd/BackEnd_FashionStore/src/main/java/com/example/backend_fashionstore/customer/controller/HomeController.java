package com.example.backend_fashionstore.customer.controller;

import com.example.backend_fashionstore.customer.dto.request.ProductFilterCustomerRequest;
import com.example.backend_fashionstore.customer.dto.response.HomeProductResponse;
import com.example.backend_fashionstore.customer.dto.response.ProductDetailCustomerResponse;
import com.example.backend_fashionstore.customer.service.CustomerProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/customer/home")
@RequiredArgsConstructor
public class HomeController {

    private final CustomerProductService customerProductService;

    @GetMapping("/products")
    public ResponseEntity<HomeProductResponse> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer colorId,
            @RequestParam(required = false) Integer sizeId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        ProductFilterCustomerRequest filter = ProductFilterCustomerRequest.builder()
                .keyword(keyword)
                .categoryId(categoryId)
                .colorId(colorId)
                .sizeId(sizeId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();
        return ResponseEntity.ok(customerProductService.getProducts(filter));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductDetailCustomerResponse> getProductDetail(@PathVariable Integer productId) {
        return ResponseEntity.ok(customerProductService.getProductDetail(productId));
    }

    @GetMapping("/latest-products")
    public ResponseEntity<HomeProductResponse> getLatestProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(customerProductService.getLatestProducts(page, size));
    }

    @GetMapping("/featured-products")
    public ResponseEntity<HomeProductResponse> getFeaturedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        return ResponseEntity.ok(customerProductService.getFeaturedProducts(page, size));
    }

    @GetMapping("/categories/{categoryId}/products")
    public ResponseEntity<HomeProductResponse> getProductsByCategory(
            @PathVariable Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(customerProductService.getProductsByCategory(categoryId, page, size));
    }
}