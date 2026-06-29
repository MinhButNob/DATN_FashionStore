package com.example.backend_fashionstore.payment.controller;

import com.example.backend_fashionstore.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

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
        paymentService.handleVnpayReturn(request);

        String responseCode = request.getParameter("vnp_ResponseCode");
        String txnRef = request.getParameter("vnp_TxnRef");

        // Redirect back to frontend
        if ("00".equals(responseCode)) {
            response.sendRedirect(frontendUrl + "/orders?payment=success");
        } else {
            response.sendRedirect(frontendUrl + "/cart?payment=failed");
        }
    }
}