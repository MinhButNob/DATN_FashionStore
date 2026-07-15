package com.example.backend_fashionstore.chat_nhan_vien.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDTO {
    private Long staffId;
    private Long userId;
    private String fullName;
    private String chatStatus; // ONLINE, OFFLINE, BUSY
    private Integer currentChatCount;
    private Integer maxChatCount;
    private Boolean isAvailable;
}