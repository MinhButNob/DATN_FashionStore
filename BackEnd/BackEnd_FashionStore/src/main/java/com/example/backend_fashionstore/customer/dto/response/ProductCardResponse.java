// ProductCardResponse.java
package com.example.backend_fashionstore.customer.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductCardResponse {
    private Integer productId;
    private String productName;
    private String description;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String mainImageUrl;
    private List<String> colorOptions;
    private Integer totalStock;
    private String categoryName;      // ✅ Thêm field này
    private Integer categoryId;        // ✅ Thêm field này
    private List<VariantCustomerResponse> variants; // ✅ Thêm để FE lấy productDetailId (wishlist, filter size...)
    private Integer soldCount;         // ✅ Số lượng đã bán thực tế (từ order_details)
    private Double averageRating;      // ✅ Điểm đánh giá trung bình thực tế (từ reviews)
    private Integer reviewCount;       // ✅ Số lượt đánh giá thực tế (từ reviews)
}