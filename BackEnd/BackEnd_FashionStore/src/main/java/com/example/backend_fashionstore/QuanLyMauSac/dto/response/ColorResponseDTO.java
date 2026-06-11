package com.example.backend_fashionstore.QuanLyMauSac.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ColorResponseDTO {
    private Integer colorId;
    private String colorCode;
    private String colorName;
    private String hexCode;
    private String status;
}