package com.example.backend_fashionstore.customer.profile.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileResponseDTO {
    private Integer customerId;
    private String fullName;
    private String email;
    private String phone;    // ✅ GIỮ PHONE
    private String address;
    private String birthday;
    private String gender;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}