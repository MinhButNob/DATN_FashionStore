package com.example.backend_fashionstore.chat_nhan_vien.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatDTO {
    private Long chatId;
    private Long customerId;
    private Long customerUserId;
    private String customerName;
    private String customerEmail;
    private String phone;
    private String address;
    private Long adminId;
    private String adminName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;
    private Integer unreadCount;
    private MessageDTO lastMessage;
    private List<MessageDTO> messages;

    // ✅ THÊM MỚI
    private String chatType; // RETURN, PRODUCT_CONSULTATION, RETURN_ONLINE, RETURN_STORE, OTHER
    private String chatTypeLabel; // Tên hiển thị
    private String transferredBy;
    private LocalDateTime transferredAt;
    private List<TransferHistoryDTO> transferHistory;
}