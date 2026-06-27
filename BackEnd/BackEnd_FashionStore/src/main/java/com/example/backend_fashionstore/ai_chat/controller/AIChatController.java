package com.example.backend_fashionstore.ai_chat.controller;

import com.example.backend_fashionstore.ai_chat.dto.ChatRequestDTO;
import com.example.backend_fashionstore.ai_chat.dto.ChatResponseDTO;
import com.example.backend_fashionstore.ai_chat.dto.ProductRecommendationDTO;
import com.example.backend_fashionstore.ai_chat.service.AIChatService;
import com.example.backend_fashionstore.ai_chat.service.ChatHistoryService;
import com.example.backend_fashionstore.ai_chat.service.ProductRecommendationService;
import com.example.backend_fashionstore.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIChatController {

    private final AIChatService aiChatService;
    private final ChatHistoryService chatHistoryService;
    private final ProductRecommendationService productRecommendationService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequestDTO request) {
        Map<String, Object> response = new HashMap<>();

        // Validate input sớm để tránh NPE sâu trong service
        if (request == null || request.getUserId() == null) {
            response.put("success", false);
            response.put("message", "Thiếu userId. Vui lòng đăng nhập lại.");
            return ResponseEntity.badRequest().body(response);
        }
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Nội dung tin nhắn không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            ChatResponseDTO chatResponse = aiChatService.processChat(request);
            response.put("success", true);
            response.put("message", "Success");
            response.put("data", chatResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // In log đầy đủ stack trace ra console BE để biết nguyên nhân thật
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage() != null ? e.getMessage() : "Lỗi không xác định, kiểm tra log server.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<Map<String, Object>> getHistory(@PathVariable Integer userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Message> history = chatHistoryService.getChatHistory(userId);
            response.put("success", true);
            response.put("message", "Success");
            response.put("data", history);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/suggest")
    public ResponseEntity<Map<String, Object>> suggestProducts(@RequestParam String keyword) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ProductRecommendationDTO> products = productRecommendationService.recommendProducts(keyword);
            response.put("success", true);
            response.put("message", "Success");
            response.put("data", products);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}