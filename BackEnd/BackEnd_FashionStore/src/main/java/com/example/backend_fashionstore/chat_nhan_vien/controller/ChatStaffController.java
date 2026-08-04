package com.example.backend_fashionstore.chat_nhan_vien.controller;

import com.example.backend_fashionstore.chat_nhan_vien.dto.ChatDTO;
import com.example.backend_fashionstore.chat_nhan_vien.dto.MessageDTO;
import com.example.backend_fashionstore.chat_nhan_vien.dto.MessageRequest;
import com.example.backend_fashionstore.chat_nhan_vien.dto.TransferChatRequest;
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

    // ===== GET CHAT TYPES =====
    @GetMapping("/types")
    public ResponseEntity<List<Map<String, String>>> getChatTypes() {
        return ResponseEntity.ok(chatService.getChatTypes());
    }

    // ===== STAFF: Lấy chat của staff =====
    @GetMapping("/{staffUserId}/chats")
    public ResponseEntity<List<ChatDTO>> getChatsByStaff(@PathVariable Long staffUserId) {
        return ResponseEntity.ok(chatService.getChatsByStaff(staffUserId));
    }

    // ===== STAFF: Lấy tất cả chat ACTIVE =====
    @GetMapping("/active")
    public ResponseEntity<List<ChatDTO>> getAllActiveChats() {
        return ResponseEntity.ok(chatService.getAllActiveChats());
    }

    // ===== ADMIN: Lấy tất cả chat =====
    @GetMapping("/admin/all")
    public ResponseEntity<List<ChatDTO>> getAllChatsForAdmin() {
        return ResponseEntity.ok(chatService.getAllChatsForAdmin());
    }

    // ===== ADMIN: Lấy chat theo trạng thái =====
    @GetMapping("/admin/status/{status}")
    public ResponseEntity<List<ChatDTO>> getChatsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(chatService.getChatsByStatus(status));
    }

    // ===== ADMIN: Lấy chat theo loại =====
    @GetMapping("/admin/type/{chatType}")
    public ResponseEntity<List<ChatDTO>> getChatsByType(@PathVariable String chatType) {
        return ResponseEntity.ok(chatService.getChatsByType(chatType));
    }

    // ===== ACCEPT CHAT =====
    @PostMapping("/{chatId}/accept/{staffUserId}")
    public ResponseEntity<?> acceptChat(@PathVariable Long chatId, @PathVariable Long staffUserId) {
        try {
            ChatDTO chat = chatService.acceptChat(chatId, staffUserId);
            webSocketHandler.sendToChatParticipants(
                    chat.getCustomerUserId() != null ? chat.getCustomerUserId().intValue() : null,
                    staffUserId.intValue(),
                    "chat_accepted",
                    chat);
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    // ===== TRANSFER CHAT (ADMIN CHUYỂN CHO STAFF KHÁC) =====
    @PostMapping("/{chatId}/transfer")
    public ResponseEntity<?> transferChat(
            @PathVariable Long chatId,
            @RequestParam Long fromUserId,
            @RequestBody TransferChatRequest request) {
        try {
            ChatDTO chat = chatService.transferChat(chatId, fromUserId, request);
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    // ===== CLOSE CHAT =====
    @PostMapping("/{chatId}/close/{staffUserId}")
    public ResponseEntity<?> closeChat(@PathVariable Long chatId, @PathVariable Long staffUserId) {
        try {
            ChatDTO chat = chatService.closeChat(chatId, staffUserId);
            webSocketHandler.sendToChatParticipants(
                    chat.getCustomerUserId() != null ? chat.getCustomerUserId().intValue() : null,
                    staffUserId.intValue(),
                    "chat_closed",
                    chat);
            webSocketHandler.broadcastToAllStaff("queue_updated",
                    Map.of("waitingCount", chatService.getWaitingCount()));
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    // ===== REOPEN CHAT =====
    @PostMapping("/{chatId}/reopen/{staffUserId}")
    public ResponseEntity<?> reopenChat(@PathVariable Long chatId, @PathVariable Long staffUserId) {
        try {
            ChatDTO chat = chatService.reopenChat(chatId, staffUserId);
            webSocketHandler.sendToChatParticipants(
                    chat.getCustomerUserId() != null ? chat.getCustomerUserId().intValue() : null,
                    staffUserId.intValue(),
                    "chat_reopened",
                    chat);
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    // ===== SEND MESSAGE =====
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

    // ===== GET MESSAGES =====
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageDTO>> getMessages(@PathVariable Long chatId) {
        return ResponseEntity.ok(chatService.getMessagesByChat(chatId));
    }

    // ===== GET QUEUE COUNT =====
    @GetMapping("/queue/count")
    public ResponseEntity<Map<String, Long>> getQueueCount() {
        return ResponseEntity.ok(Map.of("waitingCount", chatService.getWaitingCount()));
    }

    // ===== MARK AS READ =====
    @PutMapping("/{chatId}/read/{userId}")
    public ResponseEntity<Void> markAsRead(@PathVariable Long chatId, @PathVariable Long userId) {
        chatService.markMessagesAsRead(chatId, userId);
        return ResponseEntity.ok().build();
    }

    // ===== GET CHAT BY ID =====
    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDTO> getChatById(@PathVariable Long chatId) {
        return ResponseEntity.ok(chatService.getChatById(chatId));
    }

    private ResponseEntity<?> errorResponse(Exception e) {
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}