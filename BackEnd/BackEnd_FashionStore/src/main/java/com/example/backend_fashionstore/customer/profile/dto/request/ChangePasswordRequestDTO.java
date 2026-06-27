package com.example.backend_fashionstore.customer.profile.dto.request;

import lombok.Data;

@Data
public class ChangePasswordRequestDTO {
    private String currentPassword;
    private String newPassword;
}