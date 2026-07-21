package com.example.backend_fashionstore.QuanLyDiaChi.controller;

import com.example.backend_fashionstore.QuanLyDiaChi.dto.request.ProvinceRequestDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.dto.request.WardRequestDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.ProvinceResponseDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.WardResponseDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.service.AdminDiaChiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/dia-chi")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminDiaChiController {

    private final AdminDiaChiService adminDiaChiService;

    // ================================================================
    // ===== QUẢN LÝ TỈNH/THÀNH PHỐ =====
    // ================================================================

    /**
     * Lấy danh sách tất cả tỉnh/thành phố
     * GET /api/admin/dia-chi/provinces
     */
    @GetMapping("/provinces")
    public ResponseEntity<List<ProvinceResponseDTO>> getAllProvinces() {
        log.info("📋 Admin getting all provinces");
        return ResponseEntity.ok(adminDiaChiService.getAllProvinces());
    }

    /**
     * Lấy chi tiết tỉnh/thành phố theo ID
     * GET /api/admin/dia-chi/provinces/{id}
     */
    @GetMapping("/provinces/{id}")
    public ResponseEntity<ProvinceResponseDTO> getProvinceById(@PathVariable Integer id) {
        log.info("📋 Admin getting province by id: {}", id);
        return ResponseEntity.ok(adminDiaChiService.getProvinceById(id));
    }

    /**
     * Tạo mới tỉnh/thành phố
     * POST /api/admin/dia-chi/provinces
     */
    @PostMapping("/provinces")
    public ResponseEntity<ProvinceResponseDTO> createProvince(@RequestBody ProvinceRequestDTO request) {
        log.info("➕ Admin creating province: {}", request.getProvinceName());
        return ResponseEntity.ok(adminDiaChiService.createProvince(request));
    }

    /**
     * Cập nhật tỉnh/thành phố
     * PUT /api/admin/dia-chi/provinces/{id}
     */
    @PutMapping("/provinces/{id}")
    public ResponseEntity<ProvinceResponseDTO> updateProvince(
            @PathVariable Integer id,
            @RequestBody ProvinceRequestDTO request
    ) {
        log.info("✏️ Admin updating province: {}", id);
        return ResponseEntity.ok(adminDiaChiService.updateProvince(id, request));
    }

    /**
     * Xóa tỉnh/thành phố (soft delete)
     * DELETE /api/admin/dia-chi/provinces/{id}
     */
    @DeleteMapping("/provinces/{id}")
    public ResponseEntity<Void> deleteProvince(@PathVariable Integer id) {
        log.info("🗑️ Admin deleting province: {}", id);
        adminDiaChiService.deleteProvince(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Tìm kiếm tỉnh/thành phố
     * GET /api/admin/dia-chi/provinces/search?keyword=...
     */
    @GetMapping("/provinces/search")
    public ResponseEntity<List<ProvinceResponseDTO>> searchProvinces(
            @RequestParam String keyword
    ) {
        log.info("🔍 Admin searching provinces: {}", keyword);
        return ResponseEntity.ok(adminDiaChiService.searchProvinces(keyword));
    }

    // ================================================================
    // ===== QUẢN LÝ PHƯỜNG/XÃ =====
    // ================================================================

    /**
     * ✅ THÊM MỚI: Lấy danh sách tất cả phường/xã
     * GET /api/admin/dia-chi/wards
     */
    @GetMapping("/wards")
    public ResponseEntity<List<WardResponseDTO>> getAllWards() {
        log.info("📋 Admin getting all wards");
        return ResponseEntity.ok(adminDiaChiService.getAllWards());
    }

    /**
     * Lấy danh sách phường/xã theo tỉnh/thành phố
     * GET /api/admin/dia-chi/provinces/{provinceId}/wards
     */
    @GetMapping("/provinces/{provinceId}/wards")
    public ResponseEntity<List<WardResponseDTO>> getWardsByProvince(@PathVariable Integer provinceId) {
        log.info("📋 Admin getting wards for province: {}", provinceId);
        return ResponseEntity.ok(adminDiaChiService.getWardsByProvince(provinceId));
    }

    /**
     * Lấy chi tiết phường/xã theo ID
     * GET /api/admin/dia-chi/wards/{id}
     */
    @GetMapping("/wards/{id}")
    public ResponseEntity<WardResponseDTO> getWardById(@PathVariable Integer id) {
        log.info("📋 Admin getting ward by id: {}", id);
        return ResponseEntity.ok(adminDiaChiService.getWardById(id));
    }

    /**
     * Tạo mới phường/xã
     * POST /api/admin/dia-chi/wards
     */
    @PostMapping("/wards")
    public ResponseEntity<WardResponseDTO> createWard(@RequestBody WardRequestDTO request) {
        log.info("➕ Admin creating ward: {}", request.getWardName());
        return ResponseEntity.ok(adminDiaChiService.createWard(request));
    }

    /**
     * Cập nhật phường/xã
     * PUT /api/admin/dia-chi/wards/{id}
     */
    @PutMapping("/wards/{id}")
    public ResponseEntity<WardResponseDTO> updateWard(
            @PathVariable Integer id,
            @RequestBody WardRequestDTO request
    ) {
        log.info("✏️ Admin updating ward: {}", id);
        return ResponseEntity.ok(adminDiaChiService.updateWard(id, request));
    }

    /**
     * Xóa phường/xã (soft delete)
     * DELETE /api/admin/dia-chi/wards/{id}
     */
    @DeleteMapping("/wards/{id}")
    public ResponseEntity<Void> deleteWard(@PathVariable Integer id) {
        log.info("🗑️ Admin deleting ward: {}", id);
        adminDiaChiService.deleteWard(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Tìm kiếm phường/xã
     * GET /api/admin/dia-chi/wards/search?keyword=...
     */
    @GetMapping("/wards/search")
    public ResponseEntity<List<WardResponseDTO>> searchWards(
            @RequestParam String keyword
    ) {
        log.info("🔍 Admin searching wards: {}", keyword);
        return ResponseEntity.ok(adminDiaChiService.searchWards(keyword));
    }
}