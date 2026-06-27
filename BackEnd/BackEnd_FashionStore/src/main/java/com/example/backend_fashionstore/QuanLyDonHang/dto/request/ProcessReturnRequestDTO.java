package com.example.backend_fashionstore.QuanLyDonHang.dto.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessReturnRequestDTO {
    private String status;  // APPROVED hoặc REJECTED
    private String adminNote;
}