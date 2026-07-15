package com.example.backend_fashionstore.chat_nhan_vien.controller;

import com.example.backend_fashionstore.chat_nhan_vien.dto.ChatDTO;
import com.example.backend_fashionstore.chat_nhan_vien.dto.ChatRequest;
import com.example.backend_fashionstore.chat_nhan_vien.dto.MessageDTO;
import com.example.backend_fashionstore.chat_nhan_vien.dto.MessageRequest;
import com.example.backend_fashionstore.chat_nhan_vien.service.ChatNhanVienService;
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
@RequestMapping("/api/chat-nhan-vien")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatNhanVienService chatService;

    @PostMapping
    public ResponseEntity<ChatDTO> createChat(@Valid @RequestBody ChatRequest request) {
        ChatDTO chat = chatService.createChat(request);
        return new ResponseEntity<>(chat, HttpStatus.CREATED);
    }

    // SỬA: Thêm validation
    @PostMapping("/messages")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody MessageRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            MessageDTO message = chatService.sendMessage(request);
            return new ResponseEntity<>(message, HttpStatus.CREATED);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDTO> getChatById(@PathVariable Long chatId) {
        ChatDTO chat = chatService.getChatById(chatId);
        return ResponseEntity.ok(chat);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ChatDTO>> getChatsByCustomer(@PathVariable Long customerId) {
        List<ChatDTO> chats = chatService.getChatsByCustomer(customerId);
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/active")
    public ResponseEntity<List<ChatDTO>> getAllActiveChats() {
        List<ChatDTO> chats = chatService.getAllActiveChats();
        return ResponseEntity.ok(chats);
    }
//
//    @GetMapping("/admin/{adminId}")
//    public ResponseEntity<List<ChatDTO>> getChatsByAdmin(@PathVariable Long adminId) {
//        List<ChatDTO> chats = chatService.getChatsByAdmin(adminId);
//        return ResponseEntity.ok(chats);
//    }
//
//    @PutMapping("/{chatId}/assign/{adminId}")
//    public ResponseEntity<ChatDTO> assignAdmin(@PathVariable Long chatId, @PathVariable Long adminId) {
//        ChatDTO chat = chatService.assignAdmin(chatId, adminId);
//        return ResponseEntity.ok(chat);
//    }

    @PutMapping("/{chatId}/close")
    public ResponseEntity<Void> closeChat(@PathVariable Long chatId) {
        chatService.closeChat(chatId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageDTO>> getMessagesByChat(@PathVariable Long chatId) {
        List<MessageDTO> messages = chatService.getMessagesByChat(chatId);
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/{chatId}/read/{userId}")
    public ResponseEntity<Void> markMessagesAsRead(@PathVariable Long chatId, @PathVariable Long userId) {
        chatService.markMessagesAsRead(chatId, userId);
        return ResponseEntity.ok().build();
    }

//    @GetMapping("/{chatId}/unread/{userId}")
//    public ResponseEntity<Long> countUnreadMessages(@PathVariable Long chatId, @PathVariable Long userId) {
//        Long count = chatService.countUnreadMessages(chatId, userId);
//        return ResponseEntity.ok(count);
//    }

    @GetMapping("/customer/{customerId}/unread/total")
    public ResponseEntity<Long> countTotalUnreadMessages(@PathVariable Long customerId) {
        Long count = chatService.countTotalUnreadMessages(customerId);
        return ResponseEntity.ok(count);
    }
}