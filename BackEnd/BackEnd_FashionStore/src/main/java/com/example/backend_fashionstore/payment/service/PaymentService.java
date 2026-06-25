package com.example.backend_fashionstore.payment.service;

import com.example.backend_fashionstore.payment.dto.PaymentResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
public interface PaymentService {

    PaymentResponse createVnpayPayment(Long orderId, HttpServletRequest request);

    void handleVnpayReturn(HttpServletRequest request);
}