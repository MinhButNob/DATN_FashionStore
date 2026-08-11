package com.example.backend_fashionstore.QuanLyDonHang.dto.request;

import com.example.backend_fashionstore.enums.ReturnInspectionAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReturnInspectionItemDTO {
    @NotNull(message = "productDetailId là bắt buộc")
    private Integer productDetailId;
    @NotNull(message = "action là bắt buộc")
    private ReturnInspectionAction action;
    @Size(max = 500, message = "discardReason không được vượt quá 500 ký tự")
    private String discardReason;
}
