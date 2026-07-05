package com.example.backend_fashionstore.chat_nhan_vien.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatDTO {
    private Long chatId;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String phone; // ===== THÊM: Số điện thoại =====
    private String address; // ===== THÊM: Địa chỉ =====
    private Long adminId;
    private String adminName;
    private LocalDateTime createdAt;
    private String status;
    private Integer unreadCount;
    private MessageDTO lastMessage;
    private List<MessageDTO> messages;
}