package com.example.backend_fashionstore.QuanLyDiaChi.controller;

import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.ProvinceResponseDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.WardResponseDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.service.CustomerDiaChiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CustomerDiaChiController {

    private final CustomerDiaChiService customerDiaChiService;

    /**
     * Lấy danh sách tất cả tỉnh/thành phố (đang hoạt động)
     * GET /api/locations/provinces
     */
    @GetMapping("/provinces")
    public ResponseEntity<List<ProvinceResponseDTO>> getActiveProvinces() {
        log.info("📋 Getting all active provinces");
        return ResponseEntity.ok(customerDiaChiService.getActiveProvinces());
    }

    /**
     * Lấy danh sách phường/xã theo tỉnh/thành phố (đang hoạt động)
     * GET /api/locations/provinces/{provinceId}/wards
     */
    @GetMapping("/provinces/{provinceId}/wards")
    public ResponseEntity<List<WardResponseDTO>> getActiveWardsByProvince(@PathVariable Integer provinceId) {
        log.info("📋 Getting active wards for province: {}", provinceId);
        return ResponseEntity.ok(customerDiaChiService.getActiveWardsByProvince(provinceId));
    }
}