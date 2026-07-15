// ChatStaffController.java
// VỊ TRÍ: com/example/backend_fashionstore/chat_nhan_vien/controller/ChatStaffController.java (THAY THẾ file cũ)
package com.example.backend_fashionstore.chat_nhan_vien.controller;

import com.example.backend_fashionstore.chat_nhan_vien.dto.ChatDTO;
import com.example.backend_fashionstore.chat_nhan_vien.dto.MessageDTO;
import com.example.backend_fashionstore.chat_nhan_vien.dto.MessageRequest;
import com.example.backend_fashionstore.chat_nhan_vien.service.ChatNhanVienService;
import com.example.backend_fashionstore.chat_nhan_vien.websocket.ChatWebSocketHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/staff")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatStaffController {

    private final ChatNhanVienService chatService;
    private final ChatWebSocketHandler webSocketHandler;

    @GetMapping("/{staffUserId}/chats")
    public ResponseEntity<List<ChatDTO>> getChatsByStaff(@PathVariable Long staffUserId) {
        return ResponseEntity.ok(chatService.getChatsByStaff(staffUserId));
    }

    // Toàn bộ chat đang ACTIVE (mọi staff) - dùng cho dashboard kiểu danh sách hội thoại chung
    @GetMapping("/active")
    public ResponseEntity<List<ChatDTO>> getAllActiveChats() {
        return ResponseEntity.ok(chatService.getAllActiveChats());
    }

    @PostMapping("/{chatId}/accept/{staffUserId}")
    public ResponseEntity<?> acceptChat(@PathVariable Long chatId, @PathVariable Long staffUserId) {
        try {
            ChatDTO chat = chatService.acceptChat(chatId, staffUserId);
            webSocketHandler.sendToChatParticipants(
                    chat.getCustomerUserId() != null ? chat.getCustomerUserId().intValue() : null,
                    staffUserId.intValue(), "chat_accepted", chat);
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    @PostMapping("/{chatId}/close/{staffUserId}")
    public ResponseEntity<?> closeChat(@PathVariable Long chatId, @PathVariable Long staffUserId) {
        try {
            ChatDTO chat = chatService.closeChat(chatId, staffUserId);
            webSocketHandler.sendToChatParticipants(
                    chat.getCustomerUserId() != null ? chat.getCustomerUserId().intValue() : null,
                    staffUserId.intValue(), "chat_closed", chat);
            webSocketHandler.broadcastToAllStaff("queue_updated", Map.of("waitingCount", chatService.getWaitingCount()));
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    @PostMapping("/{chatId}/reopen/{staffUserId}")
    public ResponseEntity<?> reopenChat(@PathVariable Long chatId, @PathVariable Long staffUserId) {
        try {
            ChatDTO chat = chatService.reopenChat(chatId, staffUserId);
            webSocketHandler.sendToChatParticipants(
                    chat.getCustomerUserId() != null ? chat.getCustomerUserId().intValue() : null,
                    staffUserId.intValue(), "chat_reopened", chat);
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    @PostMapping("/messages")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody MessageRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }
        try {
            MessageDTO message = chatService.sendMessage(request);
            return new ResponseEntity<>(message, HttpStatus.CREATED);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageDTO>> getMessages(@PathVariable Long chatId) {
        return ResponseEntity.ok(chatService.getMessagesByChat(chatId));
    }

    @GetMapping("/queue/count")
    public ResponseEntity<Map<String, Long>> getQueueCount() {
        return ResponseEntity.ok(Map.of("waitingCount", chatService.getWaitingCount()));
    }

    @PutMapping("/{chatId}/read/{userId}")
    public ResponseEntity<Void> markAsRead(@PathVariable Long chatId, @PathVariable Long userId) {
        chatService.markMessagesAsRead(chatId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDTO> getChatById(@PathVariable Long chatId) {
        return ResponseEntity.ok(chatService.getChatById(chatId));
    }

    private ResponseEntity<?> errorResponse(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }
}