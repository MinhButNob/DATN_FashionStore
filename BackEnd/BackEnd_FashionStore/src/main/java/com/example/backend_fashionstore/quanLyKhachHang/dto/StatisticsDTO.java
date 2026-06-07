package com.example.backend_fashionstore.quanLyKhachHang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsDTO {
    private long totalCustomers;      // Tổng số khách
    private long activeCustomers;     // Đang hoạt động (ACTIVE)
    private long lockedCustomers;     // Bị khóa (BANNED)
    private long deletedCustomers;    // Đã xóa (DELETED)
}