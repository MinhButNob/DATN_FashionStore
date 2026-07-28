package com.example.backend_fashionstore.chat_nhan_vien.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private Long customerId;
    private String message;
    private Boolean isAi;
}