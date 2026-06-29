package com.example.backend_fashionstore.ai_chat.service;

import com.example.backend_fashionstore.ai_chat.repository.ChatRepository;
import com.example.backend_fashionstore.ai_chat.repository.MessageRepository;
import com.example.backend_fashionstore.entity.Chat;
import com.example.backend_fashionstore.entity.Customer;
import com.example.backend_fashionstore.entity.Message;
import com.example.backend_fashionstore.auth.repository.CustomerRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final CustomerRepository customerRepository;

    @Data
    public static class UserContext {
        private String gender; // MALE, FEMALE, UNKNOWN
        private Double height;
        private Double weight;
        private String lastCategory;
    }

    private final Map<Integer, UserContext> userContexts = new ConcurrentHashMap<>();

    @Transactional
    public Chat getOrCreateActiveChat(Integer userId) {
        if (userId == null) {
            throw new RuntimeException("userId là null, không thể tạo/lấy phiên chat.");
        }

        Customer customer = customerRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy khách hàng (Customer) liên kết với userId=" + userId +
                                ". Kiểm tra xem frontend có đang gửi đúng User.userId hay đang gửi nhầm customerId không."));

        if (customer.getUser() == null) {
            throw new RuntimeException("Customer (customerId=" + customer.getCustomerId() + ") không có liên kết User hợp lệ (customer.getUser() == null).");
        }

        // Khởi tạo UserContext nếu chưa có
        if (!userContexts.containsKey(userId)) {
            UserContext ctx = new UserContext();
            // Có thể load giới tính từ Customer nếu có trường giới tính trong DB
            // ctx.setGender(customer.getGender() != null ? customer.getGender() : "UNKNOWN");
            ctx.setGender("UNKNOWN");
            userContexts.put(userId, ctx);
        }

        Optional<Chat> activeChatOpt = chatRepository.findByCustomer_CustomerIdAndStatus(customer.getCustomerId(), "ACTIVE");

        if (activeChatOpt.isPresent()) {
            return activeChatOpt.get();
        }

        Chat newChat = new Chat();
        newChat.setCustomer(customer);
        newChat.setStatus("ACTIVE");

        return chatRepository.save(newChat);
    }

    @Transactional
    public Message saveUserMessage(Chat chat, String content) {
        Message message = new Message();
        message.setChat(chat);
        message.setUser(chat.getCustomer().getUser());
        message.setMessageContent(content);
        message.setIsAi(false);
        message.setIsRead(true);
        return messageRepository.save(message);
    }

    @Transactional
    public Message saveAIMessage(Chat chat, String content) {
        Message message = new Message();
        message.setChat(chat);
        message.setUser(chat.getCustomer().getUser());
        String safeContent = (content != null && content.length() > 2000)
                ? content.substring(0, 2000) : content;
        message.setMessageContent(safeContent);
        message.setIsAi(true);
        message.setIsRead(false);
        return messageRepository.save(message);
    }

    public List<Message> getChatHistory(Integer userId) {
        Chat chat = getOrCreateActiveChat(userId);
        return messageRepository.findByChat_ChatIdOrderBySentDateAsc(chat.getChatId());
    }

    public UserContext getUserContext(Integer userId) {
        if (userId == null) return null;
        if (!userContexts.containsKey(userId)) {
            getOrCreateActiveChat(userId);
        }
        return userContexts.get(userId);
    }

    public void updateUserContext(Integer userId, String gender, Double height, Double weight) {
        if (userId == null) return;
        UserContext ctx = getUserContext(userId);
        if (ctx != null) {
            if (gender != null && !gender.isEmpty()) ctx.setGender(gender);
            if (height != null) ctx.setHeight(height);
            if (weight != null) ctx.setWeight(weight);
        }
    }
}