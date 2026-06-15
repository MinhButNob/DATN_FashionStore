package com.example.backend_fashionstore.quanLyDonHang.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String id; // Use "DH" + orderId for frontend
    private Long originalId;
    private CustomerInfo customer;
    private List<OrderItemResponse> items;
    private BigDecimal subtotal;
    private ShippingInfo shipping;
    private BigDecimal discount;
    private BigDecimal total;
    private String date; // formatted date string
    private String status;
    private PaymentInfo payment;
    private String shippingAddress;
    private String notes;
    private List<TimelineEvent> timeline;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        private String name;
        private String phone;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingInfo {
        private String method;
        private BigDecimal fee;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private String method;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEvent {
        private String date;
        private String text;
        private String icon;
    }
}
