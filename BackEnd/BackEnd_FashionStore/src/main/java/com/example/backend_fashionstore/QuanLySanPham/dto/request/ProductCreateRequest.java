package com.example.backend_fashionstore.QuanLySanPham.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 20, message = "Tên sản phẩm không được vượt quá 20 ký tự")
    private String productName;

    @Size(max = 20, message = "Mô tả không được vượt quá 20 ký tự")
    private String description;

    @NotNull(message = "Category ID không được để trống")
    private Integer categoryId;

    private String status;

    // ✅ THÊM promotionId cho toàn bộ sản phẩm
    private Integer promotionId;

    @NotEmpty(message = "Phải có ít nhất một biến thể")
    private List<VariantRequest> variants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantRequest {
        @NotBlank(message = "SKU không được để trống")
        @Size(max = 20, message = "SKU không được vượt quá 20 ký tự")
        private String sku;

        @NotNull(message = "Color ID không được để trống")
        private Integer colorId;

        @NotNull(message = "Size ID không được để trống")
        private Integer sizeId;

        @NotNull(message = "Giá không được để trống")
        @DecimalMin(value = "0", message = "Giá phải >= 0")
        private BigDecimal price;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 0, message = "Số lượng phải >= 0")
        private Integer quantity;

        // ❌ ĐÃ XÓA promotionId
    }
}