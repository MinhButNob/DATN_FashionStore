package com.example.backend_fashionstore.chat_nhan_vien.dto;

import lombok.Data;

@Data
public class TransferChatRequest {
    private Long chatId;
    private Long toUserId; // ID của admin/nhân viên nhận chuyển
    private String reason; // Lý do chuyển
}