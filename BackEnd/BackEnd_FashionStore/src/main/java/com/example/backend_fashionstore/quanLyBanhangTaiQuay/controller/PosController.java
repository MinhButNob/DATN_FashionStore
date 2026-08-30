package com.example.backend_fashionstore.quanLyBanhangTaiQuay.controller;

import com.example.backend_fashionstore.QuanLySanPham.dto.response.ApiResponse;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto.*;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.service.PosService;
import com.example.backend_fashionstore.quanLyBanhangTaiQuay.service.PosSessionService;
import com.example.backend_fashionstore.quanLyKhachHang.dto.CustomerDTO;
import com.example.backend_fashionstore.quanLyVoucher.dto.VoucherDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosController {

    private final PosService posService;
    private final PosSessionService posSessionService;

    // ===================================================
    // QUẢN LÝ PHIÊN BÁN HÀNG (SESSION)
    // ===================================================

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/sessions/open")
    public ResponseEntity<PosSessionResponse> openSession(@RequestBody PosSessionRequest request) {
        return ResponseEntity.ok(posSessionService.openSession(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PutMapping("/sessions/{sessionId}/close")
    public ResponseEntity<PosSessionResponse> closeSession(@PathVariable Integer sessionId,
                                                           @RequestParam Integer employeeId) {
        return ResponseEntity.ok(posSessionService.closeSession(sessionId, employeeId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/sessions/active")
    public ResponseEntity<List<PosSessionResponse>> getAllActiveSessions() {
        return ResponseEntity.ok(posSessionService.getAllActiveSessions());
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/sessions/my-active")
    public ResponseEntity<PosSessionResponse> getMyActiveSession() {
        return ResponseEntity.ok(posSessionService.getMyActiveSession());
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/sessions/employee/{employeeId}/active")
    public ResponseEntity<PosSessionResponse> getActiveSessionByEmployee(@PathVariable Integer employeeId) {
        return ResponseEntity.ok(posSessionService.getActiveSessionByEmployee(employeeId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/sessions/cash-register/{cashRegisterId}/active")
    public ResponseEntity<PosSessionResponse> getActiveSessionByCashRegister(@PathVariable Integer cashRegisterId) {
        return ResponseEntity.ok(posSessionService.getActiveSessionByCashRegister(cashRegisterId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/sessions/employee/{employeeId}/history")
    public ResponseEntity<List<PosSessionResponse>> getSessionsByEmployee(@PathVariable Integer employeeId) {
        return ResponseEntity.ok(posSessionService.getSessionsByEmployee(employeeId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<PosSessionDetailResponse> getSessionDetail(@PathVariable Integer sessionId) {
        return ResponseEntity.ok(posSessionService.getSessionDetail(sessionId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/sessions/{sessionId}/stats")
    public ResponseEntity<PosSessionStatsResponse> getSessionStats(@PathVariable Integer sessionId) {
        return ResponseEntity.ok(posService.getSessionStats(sessionId));
    }

    // ===================================================
    // GIAO DỊCH TIỀN MẶT
    // ===================================================

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/sessions/cash-transaction")
    public ResponseEntity<PosCashTransactionResponse> cashTransaction(@RequestBody PosCashTransactionRequest request) {
        return ResponseEntity.ok(posSessionService.cashTransaction(request));
    }

    // ===================================================
    // QUẢN LÝ ĐƠN HÀNG
    // ===================================================

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/orders")
    public ResponseEntity<PosOrderResponse> createOrder(@RequestBody PosOrderRequest request) {
        return ResponseEntity.ok(posService.createOrder(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/orders/session/{sessionId}")
    public ResponseEntity<List<PosOrderResponse>> getOrdersBySession(@PathVariable Integer sessionId) {
        return ResponseEntity.ok(posService.getOrdersBySession(sessionId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/orders/invoice/{invoiceNumber}")
    public ResponseEntity<PosOrderResponse> getOrderByInvoiceNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(posService.getOrderByInvoiceNumber(invoiceNumber));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<PosOrderResponse> getOrderById(@PathVariable Integer orderId) {
        return ResponseEntity.ok(posService.getOrderById(orderId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/orders/{orderId}/detail")
    public ResponseEntity<PosOrderDetailResponse> getOrderDetail(@PathVariable Integer orderId) {
        return ResponseEntity.ok(posService.getOrderDetailWithProducts(orderId));
    }

    // ===================================================
    // HOÀN TIỀN / TRẢ HÀNG
    // ===================================================

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/refunds")
    public ResponseEntity<PosRefundResponse> refundOrder(@RequestBody PosRefundRequest request) {
        return ResponseEntity.ok(posService.refundOrder(request));
    }

    // ===================================================
    // IN HÓA ĐƠN
    // ===================================================

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/orders/{orderId}/print")
    public ResponseEntity<PosReceiptLogResponse> printReceipt(
            @PathVariable Integer orderId,
            @RequestParam(required = false, defaultValue = "1") Integer copies) {
        return ResponseEntity.ok(posService.printReceiptAsResponse(orderId, copies));
    }

    // ===================================================
    // VNPAY TẠI QUẦY
    // ===================================================

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/orders/{orderId}/vnpay/create-payment")
    public ResponseEntity<PosPaymentResponse> createVnpayPayment(
            @PathVariable Integer orderId,
            @RequestBody PosPaymentRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getHeader("X-FORWARDED-FOR");
        if (clientIp == null) clientIp = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(
                posService.createVnpayPaymentForPos(orderId, request.getReturnUrl(), clientIp));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/orders/{orderId}/vnpay/confirm")
    public ResponseEntity<PosPaymentResponse> confirmVnpayPayment(
            @PathVariable Integer orderId,
            @RequestBody java.util.Map<String, String> params) {
        return ResponseEntity.ok(posService.confirmVnpayForPos(orderId, params));
    }

    // ===================================================
    // TÌM ĐƠN HÀNG THEO SĐT
    // ===================================================

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/orders/search-by-phone")
    public ResponseEntity<List<PosOrderResponse>> searchOrdersByPhone(
            @RequestParam String phone) {
        // ✅ Gọi service thay vì repository
        return ResponseEntity.ok(posService.getOrdersByPhone(phone));
    }

    // ===================================================
    // QUẢN LÝ KHÁCH HÀNG
    // ===================================================

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/customers")
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        return ResponseEntity.ok(posService.getAllCustomers());
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/customers/search")
    public ResponseEntity<List<CustomerDTO>> searchCustomers(@RequestParam String keyword) {
        return ResponseEntity.ok(posService.searchCustomers(keyword));
    }

    @GetMapping("/{orderId}/voucher")
    public ResponseEntity<ApiResponse<VoucherDTO>> getVoucherByOrderId(@PathVariable Integer orderId) {
        log.info("📋 Getting voucher for order: {}", orderId);

        try {
            VoucherDTO voucher = posService.getVoucherByOrderId(orderId);
            return ResponseEntity.ok(ApiResponse.success(voucher));
        } catch (Exception e) {
            log.error("❌ Error getting voucher: {}", e.getMessage());
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }


}