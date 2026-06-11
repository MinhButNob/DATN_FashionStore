package com.example.backend_fashionstore.QuanLySanPham.dto.request;

import lombok.*;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductUpdateRequest {
    private String productName, description, brand, gender;
    private Integer categoryId;
    private Boolean isActive;
    private List<ProductCreateRequest.VariantRequest> variants;
}
