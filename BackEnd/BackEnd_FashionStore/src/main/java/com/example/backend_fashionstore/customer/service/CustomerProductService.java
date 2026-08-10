// CustomerProductService.java
package com.example.backend_fashionstore.customer.service;

import com.example.backend_fashionstore.customer.dto.request.ProductFilterCustomerRequest;
import com.example.backend_fashionstore.customer.dto.response.HomeProductResponse;
import com.example.backend_fashionstore.customer.dto.response.ProductDetailCustomerResponse;

public interface CustomerProductService {
    HomeProductResponse getProducts(ProductFilterCustomerRequest filter);
    ProductDetailCustomerResponse getProductDetail(Integer productId);
    HomeProductResponse getLatestProducts(int page, int size);
    HomeProductResponse getProductsByCategory(Integer categoryId, int page, int size);
    HomeProductResponse getFeaturedProducts(int page, int size);
    HomeProductResponse getTopRatedProducts(int page, int size);
}