package com.example.backend_fashionstore.QuanLySize.dto;

import lombok.Data;

@Data
public class SizeRequestDTO {
    private String sizeCode;
    private String sizeName;
    private String description;
    private String status;  // ✅ String: ACTIVE, INACTIVE
}