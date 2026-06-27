package com.example.backend_fashionstore.ai_chat.service;

import com.example.backend_fashionstore.ai_chat.repository.ChatRepository;
import com.example.backend_fashionstore.ai_chat.repository.MessageRepository;
import com.example.backend_fashionstore.entity.Chat;
import com.example.backend_fashionstore.entity.Customer;
import com.example.backend_fashionstore.entity.Message;
import com.example.backend_fashionstore.auth.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public Chat getOrCreateActiveChat(Integer userId) {
        if (userId == null) {
            throw new RuntimeException("userId là null, không thể tạo/lấy phiên chat.");
        }

        // Tìm Customer theo user_id (user_id là ID đăng nhập của người dùng)
        Customer customer = customerRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy khách hàng (Customer) liên kết với userId=" + userId +
                                ". Kiểm tra xem frontend có đang gửi đúng User.userId hay đang gửi nhầm customerId không."));

        if (customer.getUser() == null) {
            throw new RuntimeException("Customer (customerId=" + customer.getCustomerId() + ") không có liên kết User hợp lệ (customer.getUser() == null).");
        }

        Optional<Chat> activeChatOpt = chatRepository.findByCustomer_CustomerIdAndStatus(customer.getCustomerId(), "ACTIVE");

        if (activeChatOpt.isPresent()) {
            return activeChatOpt.get();
        }

        Chat newChat = new Chat();
        newChat.setCustomer(customer);
        newChat.setStatus("ACTIVE");
        // admin = null indicates AI/unassigned chat

        return chatRepository.save(newChat);
    }

    @Transactional
    public Message saveUserMessage(Chat chat, String content) {
        Message message = new Message();
        message.setChat(chat);
        message.setUser(chat.getCustomer().getUser()); // Tin nhắn của khách hàng
        message.setMessageContent(content);
        message.setIsAi(false);
        message.setIsRead(true);
        return messageRepository.save(message);
    }

//    @Transactional
//    public Message saveAIMessage(Chat chat, String content) {
//        Message message = new Message();
//        message.setChat(chat);
//        message.setUser(null); // Không có user cho tin nhắn AI
//        message.setMessageContent(content);
//        message.setIsAi(true);
//        message.setIsRead(false); // Khách hàng chưa đọc
//        return messageRepository.save(message);
//    }

    @Transactional
    public Message saveAIMessage(Chat chat, String content) {
        Message message = new Message();
        message.setChat(chat);
        message.setUser(chat.getCustomer().getUser());
        // Truncate content to avoid DB column overflow (most TEXT columns ~65535, but varchar may be smaller)
        String safeContent = (content != null && content.length() > 2000)
                ? content.substring(0, 2000) : content;
        message.setMessageContent(safeContent);
        message.setIsAi(true);
        message.setIsRead(false);
        return messageRepository.save(message);
    }


    public List<Message> getChatHistory(Integer userId) {
        Chat chat = getOrCreateActiveChat(userId); // userId (không phải customerId)
        return messageRepository.findByChat_ChatIdOrderBySentDateAsc(chat.getChatId());
    }
}