package com.example.backend_fashionstore.payment.service;

import com.example.backend_fashionstore.QuanLyDonHang.repository.OrderRepository;
import com.example.backend_fashionstore.config.VNPayConfig;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.enums.OrderStatus;
import com.example.backend_fashionstore.enums.PaymentStatus;
import com.example.backend_fashionstore.payment.dto.PaymentResponse;
import com.example.backend_fashionstore.payment.repository.PaymentRepository;
import com.example.backend_fashionstore.payment.repository.PaymentTransactionRepository;
import com.example.backend_fashionstore.payment.repository.PaymentMethodRepository;

import com.example.backend_fashionstore.customer.cart.repository.CartItemRepository;
import com.example.backend_fashionstore.customer.cart.repository.CartRepository;
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
    private final com.example.backend_fashionstore.QuanLyDonHang.repository.OrderDetailRepository orderDetailRepository;
    private final com.example.backend_fashionstore.QuanLySanPham.repository.ProductDetail.ProductDetailRepository productDetailRepository;
    private final com.example.backend_fashionstore.quanLyVoucher.repository.VoucherRepository voucherRepository;
    private final com.example.backend_fashionstore.customer.order.repository.OrderHistoryRepository orderHistoryRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;


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

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS || payment.getPaymentStatus() == PaymentStatus.FAILED) {
            return;
        }

        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(responseCode)) {
                payment.setPaymentStatus(PaymentStatus.SUCCESS);
                Order order = payment.getOrder();
                if(order != null) {
                    order.setOrderStatus(OrderStatus.CONFIRMED); // Update order status on success
                    order.setPaymentStatus(PaymentStatus.SUCCESS);
                    orderRepository.save(order);
                    
                    // Clear cart upon successful payment
                    try {
                        Integer customerId = order.getCustomer().getCustomerId();
                        Cart cart = cartRepository.findByCustomerIdAndDeletedAtIsNull(customerId).orElse(null);
                        if (cart != null) {
                            java.util.List<CartItem> cartItems = cartItemRepository.findByCart_CartIdAndDeletedAtIsNull(cart.getCartId());
                            if (cartItems != null && !cartItems.isEmpty()) {
                                cartItemRepository.deleteAll(cartItems);
                                cart.getCartItems().clear();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                cancelOrderOnPaymentFail(payment.getOrder());
            }
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            cancelOrderOnPaymentFail(payment.getOrder());
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
    
    private void cancelOrderOnPaymentFail(Order order) {
        if (order == null || order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setUpdatedAt(java.time.LocalDateTime.now());
        // Soft delete the order so it acts as "not placed" for the user
        order.setDeletedAt(java.time.LocalDateTime.now());
        orderRepository.save(order);

        java.util.List<com.example.backend_fashionstore.entity.OrderDetail> details = orderDetailRepository.findByOrder_OrderId(order.getOrderId());
        for (com.example.backend_fashionstore.entity.OrderDetail detail : details) {
            com.example.backend_fashionstore.entity.ProductDetail pd = detail.getProductDetail();
            if (pd != null) {
                pd.setQuantity(pd.getQuantity() + detail.getQuantity());
                productDetailRepository.save(pd);
            }
        }

        if (order.getVoucher() != null) {
            com.example.backend_fashionstore.entity.Voucher voucher = order.getVoucher();
            voucher.setVoucherUsedCount(Math.max(0, voucher.getVoucherUsedCount() - 1));
            voucherRepository.save(voucher);
        }

        com.example.backend_fashionstore.entity.OrderHistory history = new com.example.backend_fashionstore.entity.OrderHistory();
        history.setOrder(order);
        history.setOrderStatus(OrderStatus.CANCELLED.name());
        history.setNote("Đơn hàng bị hủy do thanh toán VNPay thất bại");
        history.setTimestamp(java.time.LocalDateTime.now());
        orderHistoryRepository.save(history);

        System.out.println("Order " + order.getOrderId() + " cancelled due to VNPay payment failure");
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