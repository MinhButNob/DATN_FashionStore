// DamagedProductStatsResponse.java
package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DamagedProductStatsResponse {
    private Long totalDamagedProducts;
    private Long pendingDamagedProducts;
    private Long processedDamagedProducts;
}