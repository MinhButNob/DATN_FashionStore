package com.example.backend_fashionstore.QuanLyDonHang.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReturnInspectionRequestDTO {
    @Size(max = 2000, message = "inspectionNote không được vượt quá 2000 ký tự")
    private String inspectionNote;
    @NotEmpty(message = "Phải kiểm tra ít nhất một sản phẩm")
    @Valid
    private List<ReturnInspectionItemDTO> items;
}
