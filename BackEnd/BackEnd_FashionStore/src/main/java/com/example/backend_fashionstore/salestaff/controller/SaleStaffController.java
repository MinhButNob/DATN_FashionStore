package com.example.backend_fashionstore.salestaff.controller;

import com.example.backend_fashionstore.QuanLyDonHang.dto.OrderStatisticsDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.request.ProcessReturnRequestDTO;
import com.example.backend_fashionstore.QuanLyDonHang.dto.response.*;
import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderRepository;
import com.example.backend_fashionstore.QuanLySanPham.dto.response.ApiResponse;
import com.example.backend_fashionstore.entity.Order;
import com.example.backend_fashionstore.entity.ProductDetail;
import com.example.backend_fashionstore.quanLyKhachHang.dto.CustomerDTO;
import com.example.backend_fashionstore.salestaff.dto.StockUpdateRequest;
import com.example.backend_fashionstore.salestaff.service.SaleStaffService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sale-staff")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALE_STAFF', 'ADMIN', 'STAFF')")
@CrossOrigin(origins = "http://localhost:5173")
public class SaleStaffController {

    private final SaleStaffService saleStaffService;
    private final OrderRepository orderRepository;

    // =============================================
    // QUẢN LÝ ĐƠN HÀNG (FULL CHỨC NĂNG)
    // =============================================

    /**
     * 1. Lấy tất cả đơn hàng
     */
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<AdminOrderResponseDTO>>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AdminOrderResponseDTO> orders = saleStaffService.getAllOrders(status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * 2. Lấy chi tiết đơn hàng
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<AdminOrderResponseDTO>> getOrderDetail(@PathVariable Long orderId) {
        AdminOrderResponseDTO order = saleStaffService.getOrderDetail(orderId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * 3. Cập nhật trạng thái đơn hàng
     */
    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<ApiResponse<AdminOrderResponseDTO>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request
    ) {
        AdminOrderResponseDTO order = saleStaffService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * 4. Cập nhật trạng thái thanh toán
     */
    @PutMapping("/orders/{orderId}/payment-status")
    public ResponseEntity<ApiResponse<AdminOrderResponseDTO>> updatePaymentStatus(
            @PathVariable Long orderId,
            @RequestParam String paymentStatus
    ) {
        AdminOrderResponseDTO order = saleStaffService.updatePaymentStatus(orderId, paymentStatus);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * 5. Admin hủy đơn hàng
     */
    @PutMapping("/orders/{orderId}/cancel")
    public ResponseEntity<ApiResponse<AdminOrderResponseDTO>> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, String> request
    ) {
        String reason = request != null ? request.get("reason") : "Hủy đơn hàng";
        AdminOrderResponseDTO order = saleStaffService.adminCancelOrder(orderId, reason);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * 6. Thống kê đơn hàng
     */
    @GetMapping("/orders/statistics")
    public ResponseEntity<ApiResponse<OrderStatisticsDTO>> getStatistics() {
        OrderStatisticsDTO statistics = saleStaffService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 7. Đếm theo trạng thái
     */
    @GetMapping("/orders/status/counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getOrderStatusCounts() {
        Map<String, Long> counts = saleStaffService.getOrderStatusCounts();
        return ResponseEntity.ok(ApiResponse.success(counts));
    }

    /**
     * 8. Lấy hóa đơn
     */
    @GetMapping("/orders/{orderId}/invoice")
    public ResponseEntity<ApiResponse<InvoiceDTO>> getInvoice(@PathVariable Long orderId) {
        InvoiceDTO invoice = saleStaffService.getInvoice(orderId);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    // =============================================
    // QUẢN LÝ HOÀN HÀNG
    // =============================================

    /**
     * 9. Lấy chi tiết yêu cầu hoàn hàng
     */
    @GetMapping("/orders/return-requests/order/{orderId}")
    public ResponseEntity<ApiResponse<ReturnRequestDetailDTO>> getReturnRequestByOrderId(
            @PathVariable Long orderId
    ) {
        ReturnRequestDetailDTO detail = saleStaffService.getReturnRequestByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    /**
     * 10. Xử lý yêu cầu hoàn hàng
     */
    @PutMapping("/orders/return-requests/{orderId}/process")
    public ResponseEntity<ApiResponse<ReturnRequestDetailDTO>> processReturnRequest(
            @PathVariable Long orderId,
            @RequestBody ProcessReturnRequestDTO request
    ) {
        ReturnRequestDetailDTO result = saleStaffService.processReturnRequest(
                orderId,
                request.getStatus(),
                request.getAdminNote()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 11. Xác nhận đã nhận hàng trả
     */
    @PutMapping("/orders/return-requests/{orderId}/receive")
    public ResponseEntity<ApiResponse<ReturnRequestDetailDTO>> receiveReturnedItems(
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, String> request
    ) {
        String note = request != null ? request.get("note") : null;
        ReturnRequestDetailDTO result = saleStaffService.receiveReturnedItems(orderId, note);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 12. Lấy tất cả yêu cầu hoàn hàng
     */
    @GetMapping("/orders/return-requests")
    public ResponseEntity<ApiResponse<Page<ReturnRequestDetailDTO>>> getAllReturnRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReturnRequestDetailDTO> requests = saleStaffService.getAllReturnRequests(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    // =============================================
    // QUẢN LÝ SẢN PHẨM (CHỈ CẬP NHẬT SỐ LƯỢNG)
    // =============================================

    /**
     * 13. Cập nhật số lượng tồn kho
     */
    @PutMapping("/products/stock/{productDetailId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStock(
            @PathVariable Integer productDetailId,
            @RequestBody StockUpdateRequest request
    ) {
        ProductDetail updated = saleStaffService.updateStock(productDetailId, request.getQuantity());

        Map<String, Object> response = new HashMap<>();
        response.put("productDetailId", updated.getProductDetailId());
        response.put("sku", updated.getSku());
        response.put("oldQuantity", updated.getQuantity() - request.getQuantity());
        response.put("newQuantity", updated.getQuantity());
        response.put("productName", updated.getProduct().getProductName());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 14. Tăng số lượng tồn kho
     */
    @PostMapping("/products/stock/{productDetailId}/increase")
    public ResponseEntity<ApiResponse<Map<String, Object>>> increaseStock(
            @PathVariable Integer productDetailId,
            @RequestBody StockUpdateRequest request
    ) {
        ProductDetail updated = saleStaffService.increaseStock(productDetailId, request.getQuantity());

        Map<String, Object> response = new HashMap<>();
        response.put("productDetailId", updated.getProductDetailId());
        response.put("sku", updated.getSku());
        response.put("added", request.getQuantity());
        response.put("newQuantity", updated.getQuantity());
        response.put("productName", updated.getProduct().getProductName());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 15. Giảm số lượng tồn kho
     */
    @PostMapping("/products/stock/{productDetailId}/decrease")
    public ResponseEntity<ApiResponse<Map<String, Object>>> decreaseStock(
            @PathVariable Integer productDetailId,
            @RequestBody StockUpdateRequest request
    ) {
        ProductDetail updated = saleStaffService.decreaseStock(productDetailId, request.getQuantity());

        Map<String, Object> response = new HashMap<>();
        response.put("productDetailId", updated.getProductDetailId());
        response.put("sku", updated.getSku());
        response.put("removed", request.getQuantity());
        response.put("newQuantity", updated.getQuantity());
        response.put("productName", updated.getProduct().getProductName());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 16. Lấy chi tiết biến thể sản phẩm
     */
    @GetMapping("/products/details/{productDetailId}")
    public ResponseEntity<ApiResponse<ProductDetail>> getProductDetail(
            @PathVariable Integer productDetailId
    ) {
        ProductDetail detail = saleStaffService.getProductDetail(productDetailId);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    // =============================================
    // QUẢN LÝ KHÁCH HÀNG (CHỈ XEM)
    // =============================================

    /**
     * 17. Lấy danh sách khách hàng
     */
    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<Page<CustomerDTO>>> getCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "customerId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<CustomerDTO> customers = saleStaffService.getCustomers(keyword, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    /**
     * 18. Lấy chi tiết khách hàng
     */
    @GetMapping("/customers/{id}")
    public ResponseEntity<ApiResponse<CustomerDTO>> getCustomerById(@PathVariable Integer id) {
        CustomerDTO customer = saleStaffService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    @GetMapping("/orders/{orderId}/confirmation-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfirmationStatus(
            @PathVariable Long orderId
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        Map<String, Object> response = new HashMap<>();
        response.put("isConfirmed", order.getProcessedBy() != null);
        response.put("processedBy", order.getProcessedBy());
        response.put("processedByRole", order.getProcessedByRole());
        response.put("processedAt", order.getProcessedAt());
        response.put("orderStatus", order.getOrderStatus().toString());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

