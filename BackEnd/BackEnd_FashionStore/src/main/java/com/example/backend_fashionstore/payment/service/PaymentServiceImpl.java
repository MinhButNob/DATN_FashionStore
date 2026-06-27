package com.example.backend_fashionstore.payment.service;

import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderRepository;
import com.example.backend_fashionstore.config.VNPayConfig;
import com.example.backend_fashionstore.entity.Order;
import com.example.backend_fashionstore.entity.Payment;
import com.example.backend_fashionstore.enums.OrderStatus;
import com.example.backend_fashionstore.enums.PaymentStatus;
import com.example.backend_fashionstore.payment.dto.PaymentResponse;
import com.example.backend_fashionstore.payment.repository.PaymentRepository;
import com.example.backend_fashionstore.payment.repository.PaymentTransactionRepository;
import com.example.backend_fashionstore.payment.repository.PaymentMethodRepository;
import com.example.backend_fashionstore.entity.PaymentMethod;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final VNPayConfig vnPayConfig;

    @Override
    @Transactional
    public PaymentResponse createVnpayPayment(Long orderId, HttpServletRequest request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // ✅ FIX: Tính total thủ công vì orderTotalAmount là computed column (insertable=false, updatable=false)
        BigDecimal paymentAmount = order.getOrderSubtotalAmount()
                .subtract(order.getOrderDiscountAmount())
                .add(order.getOrderShippingFee());

        String txnRef = VNPayConfig.getRandomNumber(8);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentAmount(paymentAmount); // ✅ dùng giá trị tính được
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setTxnRef(txnRef);
        payment.setPaymentGateway("VNPAY");


        PaymentMethod paymentMethod = paymentMethodRepository.findByMethodName("VNPAY")
                .orElseGet(() -> {
                    PaymentMethod newMethod = new PaymentMethod();
                    newMethod.setMethodName("VNPAY");
                    newMethod.setDescription("Thanh toán qua VNPAY");
                    return paymentMethodRepository.save(newMethod);
                });
        payment.setPaymentMethod(paymentMethod);
        
        paymentRepository.save(payment);

        long amount = paymentAmount.longValue() * 100;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnPayConfig.getVnp_Version());
        vnp_Params.put("vnp_Command", vnPayConfig.getVnp_Command());
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", txnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + txnRef);
        vnp_Params.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getVnp_ReturnUrl());
        vnp_Params.put("vnp_IpAddr", VNPayConfig.getIpAddress(request));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    // Build hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    
                    // Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        String queryUrl = query.toString();
        String secretKey = vnPayConfig.getSecretKey().trim();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(secretKey, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnPayConfig.getVnp_PayUrl().trim() + "?" + queryUrl;
        
        System.out.println("=== VNPAY PAYMENT GENERATION ===");
        System.out.println("hashData: " + hashData.toString());
        System.out.println("queryString: " + query.toString());
        System.out.println("vnp_SecureHash: " + vnp_SecureHash);
        System.out.println("paymentUrl: " + paymentUrl);
        System.out.println("================================");

        return PaymentResponse.builder()
                .txnRef(txnRef)
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    @Transactional
    public void handleVnpayReturn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }

        String signValue = hashAllFields(fields);
        
        String txnRef = request.getParameter("vnp_TxnRef");
        String responseCode = request.getParameter("vnp_ResponseCode");
        String transactionNo = request.getParameter("vnp_TransactionNo");
        String bankCode = request.getParameter("vnp_BankCode");
        String amountStr = request.getParameter("vnp_Amount");
        String orderInfo = request.getParameter("vnp_OrderInfo");

        Payment payment = paymentRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(responseCode)) {
                payment.setPaymentStatus(PaymentStatus.SUCCESS);
                Order order = payment.getOrder();
                if(order != null) {
                    order.setOrderStatus(OrderStatus.CONFIRMED); // Update order status on success
                    orderRepository.save(order);
                }
            } else {
                payment.setPaymentStatus(PaymentStatus.FAILED);
            }
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
        }
        
        paymentRepository.save(payment);

        // Save Payment Transaction Details
        com.example.backend_fashionstore.entity.PaymentTransaction paymentTransaction = new com.example.backend_fashionstore.entity.PaymentTransaction();
        paymentTransaction.setPayment(payment);
        paymentTransaction.setTransactionType("VNPAY");
        if(amountStr != null) {
            paymentTransaction.setAmount(new java.math.BigDecimal(amountStr).divide(new java.math.BigDecimal(100)));
        } else {
            paymentTransaction.setAmount(payment.getPaymentAmount());
        }
        paymentTransaction.setBankCode(bankCode);
        paymentTransaction.setTransactionId(transactionNo);
        paymentTransaction.setResponseCode(responseCode);
        paymentTransaction.setNote(orderInfo);
        paymentTransaction.setStatus(payment.getPaymentStatus() == PaymentStatus.SUCCESS ? "COMPLETED" : "FAILED");
        paymentTransactionRepository.save(paymentTransaction);
    }
    
    private String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    sb.append(fieldName);
                    sb.append("=");
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    sb.append("&");
                }
            }
        }
        return VNPayConfig.hmacSHA512(vnPayConfig.getSecretKey().trim(), sb.toString());
    }
}