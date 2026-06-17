package com.example.backend_fashionstore.quanLyBanhangTaiQuay.controller;

import com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto.*;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.service.PosService;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.service.PosSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosController {

    private final PosService posService;
    private final PosSessionService posSessionService;

    // ===================================================
    // QUẢN LÝ PHIÊN BÁN HÀNG (SESSION)
    // ===================================================

    /** Mở ca bán hàng */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/sessions/open")
    public ResponseEntity<PosSessionResponse> openSession(@RequestBody PosSessionRequest request) {
        return ResponseEntity.ok(posSessionService.openSession(request));
    }

    /** Đóng ca bán hàng */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PutMapping("/sessions/{sessionId}/close")
    public ResponseEntity<PosSessionResponse> closeSession(@PathVariable Integer sessionId,
                                                           @RequestParam Integer employeeId) {
        return ResponseEntity.ok(posSessionService.closeSession(sessionId, employeeId));
    }

    /** Lấy tất cả ca đang hoạt động */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/sessions/active")
    public ResponseEntity<List<PosSessionResponse>> getAllActiveSessions() {
        return ResponseEntity.ok(posSessionService.getAllActiveSessions());
    }

    /** Lấy ca đang hoạt động của tôi */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/sessions/my-active")
    public ResponseEntity<PosSessionResponse> getMyActiveSession() {
        return ResponseEntity.ok(posSessionService.getMyActiveSession());
    }

    /** Lấy ca đang hoạt động của nhân viên */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/sessions/employee/{employeeId}/active")
    public ResponseEntity<PosSessionResponse> getActiveSessionByEmployee(@PathVariable Integer employeeId) {
        return ResponseEntity.ok(posSessionService.getActiveSessionByEmployee(employeeId));
    }

    /** Lấy ca đang hoạt động theo quầy */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/sessions/cash-register/{cashRegisterId}/active")
    public ResponseEntity<PosSessionResponse> getActiveSessionByCashRegister(@PathVariable Integer cashRegisterId) {
        return ResponseEntity.ok(posSessionService.getActiveSessionByCashRegister(cashRegisterId));
    }

    /** Lấy lịch sử ca theo nhân viên */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/sessions/employee/{employeeId}/history")
    public ResponseEntity<List<PosSessionResponse>> getSessionsByEmployee(@PathVariable Integer employeeId) {
        return ResponseEntity.ok(posSessionService.getSessionsByEmployee(employeeId));
    }

    /** Lấy chi tiết ca bán hàng */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<PosSessionDetailResponse> getSessionDetail(@PathVariable Integer sessionId) {
        return ResponseEntity.ok(posSessionService.getSessionDetail(sessionId));
    }

    /** Lấy thống kê theo ca */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/sessions/{sessionId}/stats")
    public ResponseEntity<PosSessionStatsResponse> getSessionStats(@PathVariable Integer sessionId) {
        return ResponseEntity.ok(posService.getSessionStats(sessionId));
    }

    // ===================================================
    // GIAO DỊCH TIỀN MẶT
    // ===================================================

    /** Nạp / Rút tiền trong ca */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/sessions/cash-transaction")
    public ResponseEntity<PosCashTransactionResponse> cashTransaction(@RequestBody PosCashTransactionRequest request) {
        return ResponseEntity.ok(posSessionService.cashTransaction(request));
    }

    // ===================================================
    // QUẢN LÝ ĐƠN HÀNG
    // ===================================================

    /** Tạo đơn hàng mới tại quầy */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/orders")
    public ResponseEntity<PosOrderResponse> createOrder(@RequestBody PosOrderRequest request) {
        return ResponseEntity.ok(posService.createOrder(request));
    }

    /** Lấy danh sách đơn hàng theo session */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/orders/session/{sessionId}")
    public ResponseEntity<List<PosOrderResponse>> getOrdersBySession(@PathVariable Integer sessionId) {
        return ResponseEntity.ok(posService.getOrdersBySession(sessionId));
    }

    /** Lấy đơn hàng theo mã hóa đơn */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/orders/invoice/{invoiceNumber}")
    public ResponseEntity<PosOrderResponse> getOrderByInvoiceNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(posService.getOrderByInvoiceNumber(invoiceNumber));
    }

    /** Lấy đơn hàng theo ID */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<PosOrderResponse> getOrderById(@PathVariable Integer orderId) {
        return ResponseEntity.ok(posService.getOrderById(orderId));
    }

    /** Lấy chi tiết đơn hàng kèm thông tin sản phẩm */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/orders/{orderId}/detail")
    public ResponseEntity<PosOrderDetailResponse> getOrderDetail(@PathVariable Integer orderId) {
        return ResponseEntity.ok(posService.getOrderDetailWithProducts(orderId));
    }

    // ===================================================
    // HOÀN TIỀN / TRẢ HÀNG
    // ===================================================

    /** Hoàn tiền đơn hàng */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/refunds")
    public ResponseEntity<PosRefundResponse> refundOrder(@RequestBody PosRefundRequest request) {
        return ResponseEntity.ok(posService.refundOrder(request));
    }

    // ===================================================
    // IN HÓA ĐƠN
    // ===================================================

    /** In hóa đơn */
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/orders/{orderId}/print")
    public ResponseEntity<PosReceiptLogResponse> printReceipt(
            @PathVariable Integer orderId,
            @RequestParam(required = false, defaultValue = "1") Integer copies) {
        return ResponseEntity.ok(posService.printReceiptAsResponse(orderId, copies));
    }
}