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
@RequestMapping("/api/chat/customer")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatCustomerController {

    private final ChatNhanVienService chatService;

    // ✅ THÊM ENDPOINT NÀY
    @GetMapping("/types")
    public ResponseEntity<List<Map<String, String>>> getChatTypes() {
        return ResponseEntity.ok(chatService.getChatTypes());
    }

    @PostMapping("/create")
    public ResponseEntity<?> createChat(@Valid @RequestBody ChatRequest request) {
        try {
            ChatDTO chat = chatService.createChat(request);
            return new ResponseEntity<>(chat, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return errorResponse(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ChatDTO>> getCustomerConversations(@RequestParam Long customerId) {
        return ResponseEntity.ok(chatService.getChatsByCustomer(customerId));
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageDTO>> getMessages(@PathVariable Long chatId) {
        return ResponseEntity.ok(chatService.getMessagesByChat(chatId));
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable Long chatId,
                                         @Valid @RequestBody MessageRequest request,
                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }
        try {
            request.setChatId(chatId);
            MessageDTO message = chatService.sendMessage(request);
            return new ResponseEntity<>(message, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return errorResponse(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{chatId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long chatId, @RequestParam Long userId) {
        chatService.markMessagesAsRead(chatId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestParam Long customerId) {
        return ResponseEntity.ok(Map.of("unreadCount", chatService.countTotalUnreadMessages(customerId)));
    }

    @PutMapping("/{chatId}/close")
    public ResponseEntity<Void> closeConversation(@PathVariable Long chatId) {
        chatService.closeChat(chatId);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<?> errorResponse(Exception e, HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", e.getMessage());
        body.put("status", status.value());
        return ResponseEntity.status(status).body(body);
    }
}