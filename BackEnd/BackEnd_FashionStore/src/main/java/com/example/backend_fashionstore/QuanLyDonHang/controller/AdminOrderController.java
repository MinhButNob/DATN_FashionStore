package com.example.backend_fashionstore.QuanLyDonHang.controller;

import com.example.backend_fashionstore.QuanLyDonHang.dto.OrderStatisticsDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.request.ProcessReturnRequestDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.AdminOrderResponseDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.InvoiceDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.ReturnRequestDetailDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.UpdateOrderStatusRequest;
import com.example.backend_fashionstore.QuanLyDonHang.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    // ===== 1. LẤY TẤT CẢ ĐƠN HÀNG =====
    @GetMapping
    public ResponseEntity<Page<AdminOrderResponseDTO>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AdminOrderResponseDTO> orders = adminOrderService.getAllOrders(status, fromDate, toDate, keyword, pageable);
        return ResponseEntity.ok(orders);
    }

    // ===== 2. LẤY CHI TIẾT ĐƠN HÀNG =====
    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderResponseDTO> getOrderDetail(@PathVariable Long orderId) {
        return ResponseEntity.ok(adminOrderService.getOrderDetail(orderId));
    }

    // ===== 3. CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG =====
    @PutMapping("/{orderId}/status")
    public ResponseEntity<AdminOrderResponseDTO> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request
    ) {
        AdminOrderResponseDTO order = adminOrderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(order);
    }

    // ===== 4. CẬP NHẬT TRẠNG THÁI THANH TOÁN =====
    @PutMapping("/{orderId}/payment-status")
    public ResponseEntity<AdminOrderResponseDTO> updatePaymentStatus(
            @PathVariable Long orderId,
            @RequestParam String paymentStatus
    ) {
        AdminOrderResponseDTO order = adminOrderService.updatePaymentStatus(orderId, paymentStatus);
        return ResponseEntity.ok(order);
    }

    // ===== 5. THỐNG KÊ =====
    @GetMapping("/statistics")
    public ResponseEntity<OrderStatisticsDTO> getStatistics() {
        return ResponseEntity.ok(adminOrderService.getStatistics());
    }

    // ===== 6. ĐẾM THEO TRẠNG THÁI =====
    @GetMapping("/status/counts")
    public ResponseEntity<Map<String, Long>> getOrderStatusCounts() {
        return ResponseEntity.ok(adminOrderService.getOrderStatusCounts());
    }

    // ================================================================
    // ===== 🆕 QUẢN LÝ HOÀN HÀNG =====
    // ================================================================

    /**
     * 7. Lấy chi tiết yêu cầu hoàn hàng theo orderId
     */
    @GetMapping("/return-requests/order/{orderId}")
    public ResponseEntity<ReturnRequestDetailDTO> getReturnRequestByOrderId(
            @PathVariable Long orderId
    ) {
        log.info("📋 Admin getting return request for order: {}", orderId);
        ReturnRequestDetailDTO detail = adminOrderService.getReturnRequestByOrderId(orderId);
        return ResponseEntity.ok(detail);
    }

    /**
     * 8. Xử lý yêu cầu hoàn hàng (Duyệt/Từ chối)
     */
    @PutMapping("/return-requests/{orderId}/process")
    public ResponseEntity<ReturnRequestDetailDTO> processReturnRequest(
            @PathVariable Long orderId,
            @RequestBody ProcessReturnRequestDTO request
    ) {
        log.info("🔄 Admin processing return request for order: {}, status: {}",
                orderId, request.getStatus());

        ReturnRequestDetailDTO result = adminOrderService.processReturnRequest(
                orderId,
                request.getStatus(),
                request.getAdminNote()
        );

        log.info("✅ Return request processed: {}", result.getStatus());
        return ResponseEntity.ok(result);
    }

    /**
     * 9. 🆕 Xác nhận đã nhận hàng trả
     */
    @PutMapping("/return-requests/{orderId}/receive")
    public ResponseEntity<ReturnRequestDetailDTO> receiveReturnedItems(
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, String> request
    ) {
        log.info("📦 Admin received returned items for order: {}", orderId);
        String note = request != null ? request.get("note") : null;
        ReturnRequestDetailDTO result = adminOrderService.receiveReturnedItems(orderId, note);
        log.info("✅ Returned items received for order: {}", orderId);
        return ResponseEntity.ok(result);
    }

    /**
     * 10. Lấy tất cả yêu cầu hoàn hàng (có phân trang)
     */
    @GetMapping("/return-requests")
    public ResponseEntity<Page<ReturnRequestDetailDTO>> getAllReturnRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReturnRequestDetailDTO> requests = adminOrderService.getAllReturnRequests(status, pageable);
        return ResponseEntity.ok(requests);
    }

    // ===== 7. LẤY HÓA ĐƠN =====
    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable Long orderId) {
        log.info("📄 Getting invoice for order: {}", orderId);
        InvoiceDTO invoice = adminOrderService.getInvoice(orderId);
        return ResponseEntity.ok(invoice);
    }
}