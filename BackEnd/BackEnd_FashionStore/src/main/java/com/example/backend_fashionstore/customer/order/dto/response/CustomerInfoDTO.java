package com.example.backend_fashionstore.customer.order.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerInfoDTO {
    private Integer customerId;
    private String customerName;
    private String phone;
    private String email;
}