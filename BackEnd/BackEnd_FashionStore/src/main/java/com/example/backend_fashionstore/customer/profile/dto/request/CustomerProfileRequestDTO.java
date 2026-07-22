package com.example.backend_fashionstore.customer.profile.dto.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileRequestDTO {
    private String fullName;
    private String email;
    private String phone;
    private String address;  // Giữ lại để dùng cho các chức năng khác
    private String birthday;
    private String gender;
    // ĐÃ XÓA CÁC TRƯỜNG: city, district, ward, streetAddress, provinceId, wardId
}