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
public class TransferHistoryDTO {
    private String fromUser;
    private String toUser;
    private LocalDateTime transferredAt;
    private String reason;
}