package com.example.backend_fashionstore.QuanLySanPham.dto.request;

import jakarta.validation.Valid;
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
    @Size(min = 5, max = 50, message = "Tên sản phẩm phải từ 5 đến 50 ký tự")
    private String productName;

    @Size(max = 100, message = "Mô tả không được vượt quá 100 ký tự")
    private String description;

    @DecimalMin(value = "0.1", message = "Khối lượng phải lớn hơn 0")
    private Double weight;

    @NotNull(message = "Category ID không được để trống")
    private Integer categoryId;

    private String status;

    private Integer promotionId;

    @NotEmpty(message = "Phải có ít nhất một biến thể")
    @Valid
    private List<VariantRequest> variants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantRequest {
//        @NotBlank(message = "Tên sản phẩm không được để trống")
        @Size(min = 5, max = 50, message = "Tên sản phẩm phải từ 5 đến 50 ký tự")
        @Pattern(regexp = "^[a-zA-Z0-9\\sÀ-ỹà-ỹđĐ]+$", message = "Tên sản phẩm không được chứa ký tự đặc biệt")
        private String productName;

        @NotBlank(message = "SKU không được để trống")
        @Size(max = 20, message = "SKU không được vượt quá 20 ký tự")
        private String sku;

        @NotNull(message = "Color ID không được để trống")
        private Integer colorId;

        @NotNull(message = "Size ID không được để trống")
        private Integer sizeId;

        @NotNull(message = "Giá không được để trống")
        @DecimalMin(value = "50000", message = "Giá sản phẩm phải từ 50.000đ trở lên")
        private BigDecimal price;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng sản phẩm tối thiểu là 1")
        private Integer quantity;

        // ✅ Danh sách khuyến mãi cho từng biến thể
        private List<Integer> promotionIds;
    }
}
