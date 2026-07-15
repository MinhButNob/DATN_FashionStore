// ChatDTO.java
// VỊ TRÍ: com/example/backend_fashionstore/chat_nhan_vien/dto/ChatDTO.java (THAY THẾ file cũ)
package com.example.backend_fashionstore.chat_nhan_vien.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatDTO {
    private Long chatId;

    // Customer.customerId (PK bảng customer) - dùng để hiển thị, KHÔNG dùng để gửi WS
    private Long customerId;

    // THÊM: user.userId của khách hàng - dùng để gửi WebSocket (sendToChatParticipants)
    // vì session WS được đăng ký theo user.userId, không phải Customer.customerId.
    // Tách riêng field này để tránh nhầm lẫn 2 loại ID như bug cũ.
    private Long customerUserId;

    private String customerName;
    private String customerEmail;
    private String phone;
    private String address;
    private Long adminId; // = admin.userId (user.userId của staff)
    private String adminName;
    private LocalDateTime createdAt;
    private String status;
    private Integer unreadCount;
    private MessageDTO lastMessage;
    private List<MessageDTO> messages;
}