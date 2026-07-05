package com.example.backend_fashionstore.chat_nhan_vien.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageDTO {
    private Long messageId;
    private Long chatId;
    private Long userId;
    private String userName;
    private String userRole;
    private String messageContent;
    private LocalDateTime sentDate;
    private Boolean isRead;
    private Boolean isAi;
}