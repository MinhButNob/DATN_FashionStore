package com.example.backend_fashionstore.payment.controller;

import com.example.backend_fashionstore.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostMapping("/vnpay/create/{orderId}")
    public ResponseEntity<?> createPayment(
            @PathVariable Long orderId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                paymentService.createVnpayPayment(orderId, request)
        );
    }

    @GetMapping("/vnpay-return")
    public void vnpayReturn(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String responseCode = request.getParameter("vnp_ResponseCode");
        try {
            paymentService.handleVnpayReturn(request);
        } catch (Exception e) {
            // Không để callback VNPay trả 500/stacktrace trắng trang; ghi log để tra cứu
            // và đưa người dùng về màn hình thanh toán thất bại.
            log.error("Xử lý callback VNPay thất bại, txnRef={}", request.getParameter("vnp_TxnRef"), e);
            responseCode = "99";
        }

        // Redirect back to frontend
        if ("00".equals(responseCode)) {
            response.sendRedirect(frontendUrl + "/orders?payment=success");
        } else {
            response.sendRedirect(frontendUrl + "/cart?payment=failed");
        }
    }
}
