package com.example.backend_fashionstore.QuanLyDonHang.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {
    private String orderStatus;
    private String notes;
    private String processedBy;
    private String processedByRole;
}