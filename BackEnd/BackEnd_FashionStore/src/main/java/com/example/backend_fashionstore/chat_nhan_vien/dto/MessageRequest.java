package com.example.backend_fashionstore.chat_nhan_vien.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class MessageRequest {
    @NotNull(message = "chatId không được để trống")
    private Long chatId;

    @NotNull(message = "userId không được để trống")
    private Long userId;

    @NotNull(message = "message không được để trống")
    private String message;

    private Boolean isAi = false;
}