package com.example.backend_fashionstore.quanLyBanhangTaiQuay.controller;

import com.example.backend_fashionstore.QuanLySanPham.dto.response.ApiResponse;
import com.example.backend_fashionstore.entity.DamagedProduct;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto.DamagedProductStatsResponse;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.service.PosReturnExchangeService;
import com.example.backend_fashionstore.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/damaged-products")
@RequiredArgsConstructor
public class DamagedProductController {

    private final PosReturnExchangeService returnExchangeService;

    /**
     * Lấy danh sách tất cả sản phẩm hỏng
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<DamagedProduct>>> getAllDamagedProducts() {
        log.info("📋 Lấy danh sách tất cả sản phẩm hỏng");
        List<DamagedProduct> products = returnExchangeService.getAllDamagedProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Lấy danh sách sản phẩm hỏng theo trạng thái
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<DamagedProduct>>> getDamagedProductsByStatus(
            @PathVariable String status) {
        log.info("📋 Lấy danh sách sản phẩm hỏng theo trạng thái: {}", status);
        List<DamagedProduct> products = returnExchangeService.getDamagedProductsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Lấy danh sách sản phẩm hỏng đang chờ xử lý
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<DamagedProduct>>> getPendingDamagedProducts() {
        log.info("📋 Lấy danh sách sản phẩm hỏng đang chờ xử lý");
        List<DamagedProduct> products = returnExchangeService.getPendingDamagedProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Lấy chi tiết sản phẩm hỏng theo ID
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @GetMapping("/{damagedProductId}")
    public ResponseEntity<ApiResponse<DamagedProduct>> getDamagedProductById(
            @PathVariable Integer damagedProductId) {
        log.info("📋 Lấy chi tiết sản phẩm hỏng ID: {}", damagedProductId);
        DamagedProduct product = returnExchangeService.getDamagedProductById(damagedProductId);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    /**
     * Cập nhật trạng thái sản phẩm hỏng
     * Nhận request body với status và notes
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{damagedProductId}/status")
    public ResponseEntity<ApiResponse<DamagedProduct>> updateDamagedProductStatus(
            @PathVariable Integer damagedProductId,
            @RequestBody Map<String, Object> request) {

        String status = (String) request.get("status");
        String notes = (String) request.get("notes");

        log.info("📋 Cập nhật trạng thái sản phẩm hỏng ID: {}, Status: {}", damagedProductId, status);

        // ✅ Chỉ truyền 3 tham số
        DamagedProduct product = returnExchangeService.updateDamagedProductStatus(
                damagedProductId,
                status,
                notes
        );

        return ResponseEntity.ok(ApiResponse.success(product));
    }

    /**
     * Xử lý hàng loạt sản phẩm hỏng
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/batch/status")
    public ResponseEntity<ApiResponse<List<DamagedProduct>>> batchUpdateDamagedProductStatus(
            @RequestBody Map<String, Object> request) {

        List<Integer> damagedProductIds = (List<Integer>) request.get("damagedProductIds");
        String status = (String) request.get("status");
        String notes = (String) request.get("notes");

        log.info("📋 Xử lý hàng loạt {} sản phẩm hỏng, Status: {}",
                damagedProductIds != null ? damagedProductIds.size() : 0, status);

        // ✅ Chỉ truyền 3 tham số
        List<DamagedProduct> products = returnExchangeService.batchUpdateDamagedProducts(
                damagedProductIds,
                status,
                notes
        );

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Lấy thống kê sản phẩm hỏng
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DamagedProductStatsResponse>> getDamagedProductStats() {
        log.info("📋 Lấy thống kê sản phẩm hỏng");
        DamagedProductStatsResponse stats = returnExchangeService.getDamagedProductStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}