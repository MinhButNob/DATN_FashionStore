package com.example.backend_fashionstore.QuanLyDonHang.dto.response;

import com.example.backend_fashionstore.enums.ReturnInspectionAction;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReturnInspectionResponseDTO {
    private Long returnRequestId;
    private Long orderId;
    private String orderStatus;
    private Map<String, ReturnInspectionResultItem> inspectionResult;
    private String inspectionNote;
    private Integer inspectedBy;
    private LocalDateTime inspectedAt;
    private List<ReturnInspectionResultItem> items;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReturnInspectionResultItem {
        private Integer productDetailId;
        private String productName;
        private String colorName;
        private String sizeName;
        private String imageUrl;
        private Integer quantity;
        private ReturnInspectionAction action;
        private String discardReason;
    }
}
