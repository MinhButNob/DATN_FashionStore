package com.example.backend_fashionstore.chat_nhan_vien.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatQueueDTO {
    private Long queueId;
    private Long customerId;
    private String customerName;
    private Long chatId;
    private String status;
    private Integer priority;
    private LocalDateTime createdAt;
    private Long position;
}