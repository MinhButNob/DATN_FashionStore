package com.example.backend_fashionstore.customer.order.dto.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

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
}