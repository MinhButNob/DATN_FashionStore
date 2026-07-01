package com.example.backend_fashionstore.chat_nhan_vien.controller;

import com.example.backend_fashionstore.chat_nhan_vien.dto.ChatDTO;
import com.example.backend_fashionstore.chat_nhan_vien.dto.ChatRequest;
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
@RequestMapping("/api/chat/customer")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatCustomerController {

    private final ChatNhanVienService chatService;
    private final ChatWebSocketHandler webSocketHandler;

    @PostMapping("/create")
    public ResponseEntity<?> createChat(@Valid @RequestBody ChatRequest request) {
        try {
            System.out.println("Creating chat for customerId: " + request.getCustomerId());
            ChatDTO chat = chatService.createChat(request);
            return new ResponseEntity<>(chat, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("customerId", request.getCustomerId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Lỗi server: " + e.getMessage());
            errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ChatDTO>> getCustomerConversations(@RequestParam Long customerId) {
        System.out.println("Getting conversations for customerId: " + customerId);
        List<ChatDTO> chats = chatService.getChatsByCustomer(customerId);
        System.out.println("Found " + chats.size() + " conversations");
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageDTO>> getMessages(@PathVariable Long chatId) {
        List<MessageDTO> messages = chatService.getMessagesByChat(chatId);
        return ResponseEntity.ok(messages);
    }

    // ===== SỬA: Gửi WebSocket từ Controller =====
    @PostMapping("/{chatId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long chatId,
            @Valid @RequestBody MessageRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            System.out.println("Sending message for chatId: " + chatId + ", userId: " + request.getUserId());
            request.setChatId(chatId);

            MessageDTO message = chatService.sendMessage(request);

            // ===== Gửi WebSocket notification =====
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("chatId", chatId);
            messageData.put("message", message);
            messageData.put("senderType", message.getUserRole());
            webSocketHandler.notifyStaffNewMessage(messageData);

            return new ResponseEntity<>(message, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("chatId", chatId);
            errorResponse.put("userId", request.getUserId());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Lỗi server: " + e.getMessage());
            errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{chatId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long chatId, @RequestParam Long userId) {
        chatService.markMessagesAsRead(chatId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestParam Long customerId) {
        Long count = chatService.countTotalUnreadMessages(customerId);
        Map<String, Long> response = new HashMap<>();
        response.put("unreadCount", count);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{chatId}/close")
    public ResponseEntity<Void> closeConversation(@PathVariable Long chatId) {
        chatService.closeChat(chatId);
        return ResponseEntity.ok().build();
    }
}