package com.example.backend_fashionstore.customer.order.controller;

import com.example.backend_fashionstore.customer.order.dto.request.OrderRequestDTO;
import com.example.backend_fashionstore.customer.order.dto.request.ReturnRequestDTO;
import com.example.backend_fashionstore.customer.order.dto.response.OrderResponseDTO;
import com.example.backend_fashionstore.customer.order.dto.response.ReturnRequestResponseDTO;
import com.example.backend_fashionstore.customer.order.service.CustomerOrderService;
import com.example.backend_fashionstore.quanLyKhachHang.repository.KhachHangCustomerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CustomerOrderController {

    private final CustomerOrderService orderService;
    private final KhachHangCustomerRepository customerRepository;

    // ===== 1. TẠO ĐƠN HÀNG =====
    @PostMapping("/create")
    public ResponseEntity<OrderResponseDTO> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody OrderRequestDTO request
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        log.info("📝 Creating order for customer: {}, voucherId: {}", customerId, request.getVoucherId());
        OrderResponseDTO order = orderService.createOrder(customerId, request);
        log.info("✅ Order created: {}, discount: {}, total: {}",
                order.getOrderId(), order.getDiscountAmount(), order.getTotalAmount());
        return ResponseEntity.ok(order);
    }

    // ===== 2. LẤY DANH SÁCH ĐƠN HÀNG =====
    @GetMapping
    public ResponseEntity<Page<OrderResponseDTO>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponseDTO> orders = orderService.getMyOrders(customerId, status, pageable);

        orders.getContent().forEach(order -> {
            log.info("📋 Order #{}: subtotal={}, discount={}, total={}, voucher={}",
                    order.getOrderId(),
                    order.getSubtotalAmount(),
                    order.getDiscountAmount(),
                    order.getTotalAmount(),
                    order.getVoucher() != null ? order.getVoucher().getVoucherCode() : "null");
        });

        return ResponseEntity.ok(orders);
    }

    // ===== 3. LẤY CHI TIẾT ĐƠN HÀNG =====
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        OrderResponseDTO order = orderService.getOrder(orderId, customerId);
        return ResponseEntity.ok(order);
    }

    // ===== 4. HỦY ĐƠN HÀNG =====
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId,
            @RequestParam String reason
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        OrderResponseDTO order = orderService.cancelOrder(orderId, customerId, reason);
        return ResponseEntity.ok(order);
    }

    // ===== 5. YÊU CẦU HOÀN HÀNG (CÓ ẢNH) =====
    @PostMapping("/{orderId}/return-request")
    public ResponseEntity<ReturnRequestResponseDTO> requestReturn(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId,
            @Valid @RequestBody ReturnRequestDTO request
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        log.info("🔄 Customer {} requesting return for order {}", customerId, orderId);
        ReturnRequestResponseDTO response = orderService.requestReturn(orderId, customerId, request);
        log.info("✅ Return request submitted for order {}", orderId);
        return ResponseEntity.ok(response);
    }

    // ===== 6. LẤY DANH SÁCH YÊU CẦU HOÀN HÀNG =====
    @GetMapping("/return-requests")
    public ResponseEntity<Page<ReturnRequestResponseDTO>> getMyReturnRequests(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReturnRequestResponseDTO> requests = orderService.getMyReturnRequests(customerId, pageable);
        return ResponseEntity.ok(requests);
    }

    // ===== HELPER METHOD =====
    private Integer getCustomerIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }
        String email = userDetails.getUsername();
        return customerRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với email: " + email))
                .getCustomerId();
    }
}