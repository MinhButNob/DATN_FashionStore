package com.example.backend_fashionstore.QuanLySanPham.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterRequest {
    private String keyword;
    private Integer categoryId;
    private String status;
    @Builder.Default private Integer page = 0;
    @Builder.Default private Integer size = 10;
    @Builder.Default private String sortBy = "productId";
    @Builder.Default private String sortDir = "DESC";
}