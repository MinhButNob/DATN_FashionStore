package com.example.backend_fashionstore.QuanLyMauSac.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColorRequestDTO {
    private String colorCode;
    private String colorName;
    private String hexCode;
    private String status;
}