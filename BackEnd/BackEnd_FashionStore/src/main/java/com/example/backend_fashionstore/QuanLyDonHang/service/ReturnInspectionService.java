package com.example.backend_fashionstore.QuanLyDonHang.service;

import com.example.backend_fashionstore.QuanLyDonHang.dto.request.ReturnInspectionRequestDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.ReturnInspectionResponseDTO;

public interface ReturnInspectionService {
    ReturnInspectionResponseDTO inspect(Long returnRequestId, ReturnInspectionRequestDTO request);
}
