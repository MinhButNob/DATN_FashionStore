package com.example.backend_fashionstore.customer.order.dto.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestDTO {
    @NotBlank(message = "Vui lòng chọn lý do hoàn hàng")
    private String reason;

    @NotBlank(message = "Vui lòng nhập mô tả chi tiết")
    private String description;

    private List<String> images;

    // ✅ THÊM: Danh sách productDetailIds cần hoàn
    @NotEmpty(message = "Vui lòng chọn sản phẩm cần hoàn")
    private List<Integer> productDetailIds;

    // ✅ THÊM: Map lưu số lượng hoàn cho từng sản phẩm
    private Map<Integer, Integer> quantities; // key: productDetailId, value: quantity
}