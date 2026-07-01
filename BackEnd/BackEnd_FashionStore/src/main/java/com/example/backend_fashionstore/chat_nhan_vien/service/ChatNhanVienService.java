package com.example.backend_fashionstore.chat_nhan_vien.service;

import com.example.backend_fashionstore.auth.repository.CustomerRepository;
import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.chat_nhan_vien.dto.ChatDTO;
import com.example.backend_fashionstore.chat_nhan_vien.dto.ChatRequest;
import com.example.backend_fashionstore.chat_nhan_vien.dto.MessageDTO;
import com.example.backend_fashionstore.chat_nhan_vien.dto.MessageRequest;
import com.example.backend_fashionstore.chat_nhan_vien.repository.ChatNhanVienRepository;
import com.example.backend_fashionstore.chat_nhan_vien.repository.MessageNhanVienRepository;
import com.example.backend_fashionstore.entity.Chat;
import com.example.backend_fashionstore.entity.Customer;
import com.example.backend_fashionstore.entity.Message;
import com.example.backend_fashionstore.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatNhanVienService {

    private final ChatNhanVienRepository chatRepository;
    private final MessageNhanVienRepository messageRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public ChatDTO createChat(ChatRequest request) {
        Integer userId = request.getCustomerId().intValue();

        System.out.println("Creating chat for userId: " + userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Customer customer = customerRepository.findByUser_UserId(userId)
                .orElseGet(() -> {
                    System.out.println("Customer not found, creating new customer for userId: " + userId);

                    Customer newCustomer = new Customer();
                    newCustomer.setUser(user);
                    newCustomer.setCustomerName(user.getUsername() != null ? user.getUsername() : "Khách hàng " + userId);
                    newCustomer.setPhone(null);
                    newCustomer.setAddress(null);

                    return customerRepository.save(newCustomer);
                });

        System.out.println("Customer found/created with ID: " + customer.getCustomerId());

        List<Chat> activeChats = chatRepository.findByCustomer_CustomerIdAndStatus(customer.getCustomerId(), "ACTIVE");
        if (!activeChats.isEmpty()) {
            Chat existingChat = activeChats.get(0);
            System.out.println("Customer already has active chat with ID: " + existingChat.getChatId());

            if (request.getMessage() != null && !request.getMessage().isEmpty()) {
                Message message = new Message();
                message.setChat(existingChat);
                message.setUser(user);
                message.setMessageContent(request.getMessage());
                message.setIsAi(request.getIsAi() != null && request.getIsAi());
                message.setIsRead(false);
                message.setSentDate(LocalDateTime.now());
                messageRepository.save(message);
            }

            return convertToDTO(existingChat);
        }

        Chat chat = new Chat();
        chat.setCustomer(customer);
        chat.setStatus("ACTIVE");
        chat.setCreatedAt(LocalDateTime.now());

        if (request.getMessage() != null && !request.getMessage().isEmpty()) {
            Chat savedChat = chatRepository.save(chat);

            Message message = new Message();
            message.setChat(savedChat);
            message.setUser(user);
            message.setMessageContent(request.getMessage());
            message.setIsAi(request.getIsAi() != null && request.getIsAi());
            message.setIsRead(false);
            message.setSentDate(LocalDateTime.now());

            messageRepository.save(message);
            return convertToDTO(savedChat);
        }

        Chat savedChat = chatRepository.save(chat);
        return convertToDTO(savedChat);
    }

    public MessageDTO sendMessage(MessageRequest request) {
        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        Integer userId = request.getUserId().intValue();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Message message = new Message();
        message.setChat(chat);
        message.setUser(user);
        message.setMessageContent(request.getMessage());
        message.setIsAi(request.getIsAi() != null && request.getIsAi());
        message.setIsRead(false);
        message.setSentDate(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);
        return convertToMessageDTO(savedMessage);
    }

    public ChatDTO getChatById(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        return convertToDTO(chat);
    }

    // ===== SỬA: Lấy tất cả chat của customer - Cập nhật thông tin mới nhất =====
    public List<ChatDTO> getChatsByCustomer(Long customerId) {
        Integer customerIdInt = customerId.intValue();
        List<Chat> chats = chatRepository.findByCustomer_CustomerId(customerIdInt);
        return chats.stream()
                .map(chat -> {
                    // ===== Lấy thông tin customer mới nhất từ database =====
                    Customer latestCustomer = customerRepository.findById(chat.getCustomer().getCustomerId())
                            .orElse(chat.getCustomer());
                    chat.setCustomer(latestCustomer);
                    return convertToDTO(chat);
                })
                .collect(Collectors.toList());
    }

    // ===== SỬA: Lấy tất cả chat đang active - Cập nhật thông tin mới nhất =====
    public List<ChatDTO> getAllActiveChats() {
        List<Chat> chats = chatRepository.findByStatus("ACTIVE");
        return chats.stream()
                .map(chat -> {
                    // ===== Lấy thông tin customer mới nhất từ database =====
                    if (chat.getCustomer() != null) {
                        Customer latestCustomer = customerRepository.findById(chat.getCustomer().getCustomerId())
                                .orElse(chat.getCustomer());
                        chat.setCustomer(latestCustomer);
                    }
                    return convertToDTO(chat);
                })
                .collect(Collectors.toList());
    }

    // ===== SỬA: Lấy chat của admin - Cập nhật thông tin mới nhất =====
    public List<ChatDTO> getChatsByAdmin(Long adminId) {
        Integer adminIdInt = adminId.intValue();
        List<Chat> chats = chatRepository.findByAdmin_UserIdAndStatus(adminIdInt, "ACTIVE");
        return chats.stream()
                .map(chat -> {
                    // ===== Lấy thông tin customer mới nhất từ database =====
                    if (chat.getCustomer() != null) {
                        Customer latestCustomer = customerRepository.findById(chat.getCustomer().getCustomerId())
                                .orElse(chat.getCustomer());
                        chat.setCustomer(latestCustomer);
                    }
                    return convertToDTO(chat);
                })
                .collect(Collectors.toList());
    }

    public ChatDTO assignAdmin(Long chatId, Long adminId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        Integer adminIdInt = adminId.intValue();
        User admin = userRepository.findById(adminIdInt)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        chat.setAdmin(admin);
        Chat updatedChat = chatRepository.save(chat);
        return convertToDTO(updatedChat);
    }

    public void closeChat(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        chat.setStatus("CLOSED");
        chatRepository.save(chat);
    }

    public List<MessageDTO> getMessagesByChat(Long chatId) {
        List<Message> messages = messageRepository.findByChat_ChatIdOrderBySentDateAsc(chatId);
        return messages.stream()
                .map(this::convertToMessageDTO)
                .collect(Collectors.toList());
    }

    public void markMessagesAsRead(Long chatId, Long userId) {
        Integer userIdInt = userId.intValue();
        messageRepository.markAllAsRead(chatId, userIdInt);
    }

    public Long countUnreadMessages(Long chatId, Long userId) {
        Integer userIdInt = userId.intValue();
        return messageRepository.countUnreadMessages(chatId, userIdInt);
    }

    public Long countTotalUnreadMessages(Long customerId) {
        Integer customerIdInt = customerId.intValue();
        List<Chat> chats = chatRepository.findByCustomer_CustomerIdAndStatus(customerIdInt, "ACTIVE");
        long total = 0;
        for (Chat chat : chats) {
            if (chat != null && chat.getCustomer() != null && chat.getCustomer().getUser() != null) {
                total += messageRepository.countUnreadMessages(chat.getChatId(),
                        chat.getCustomer().getUser().getUserId());
            }
        }
        return total;
    }

    // ===== SỬA: convertToDTO - Lấy thông tin mới nhất =====
    private ChatDTO convertToDTO(Chat chat) {
        if (chat == null) {
            return null;
        }

        ChatDTO dto = new ChatDTO();
        dto.setChatId(chat.getChatId());

        if (chat.getCustomer() != null) {
            dto.setCustomerId(chat.getCustomer().getCustomerId().longValue());
            dto.setCustomerName(chat.getCustomer().getCustomerName());
            dto.setPhone(chat.getCustomer().getPhone()); // ===== THÊM: Số điện thoại =====
            dto.setAddress(chat.getCustomer().getAddress()); // ===== THÊM: Địa chỉ =====
            if (chat.getCustomer().getUser() != null) {
                dto.setCustomerEmail(chat.getCustomer().getUser().getEmail());
            }
        }

        dto.setCreatedAt(chat.getCreatedAt());
        dto.setStatus(chat.getStatus());

        if (chat.getAdmin() != null) {
            dto.setAdminId(chat.getAdmin().getUserId().longValue());
            dto.setAdminName(chat.getAdmin().getUsername());
        }

        List<Message> lastMessages = messageRepository.findLastMessage(chat.getChatId(),
                PageRequest.of(0, 1));
        if (!lastMessages.isEmpty() && lastMessages.get(0) != null) {
            dto.setLastMessage(convertToMessageDTO(lastMessages.get(0)));
        }

        if (chat.getCustomer() != null && chat.getCustomer().getUser() != null) {
            Long unreadCount = messageRepository.countUnreadMessages(chat.getChatId(),
                    chat.getCustomer().getUser().getUserId());
            dto.setUnreadCount(unreadCount != null ? unreadCount.intValue() : 0);
        } else {
            dto.setUnreadCount(0);
        }

        return dto;
    }

    private MessageDTO convertToMessageDTO(Message message) {
        if (message == null) {
            return null;
        }

        MessageDTO dto = new MessageDTO();
        dto.setMessageId(message.getMessageId());

        if (message.getChat() != null) {
            dto.setChatId(message.getChat().getChatId());
        }

        if (message.getUser() != null) {
            dto.setUserId(message.getUser().getUserId().longValue());
            dto.setUserName(message.getUser().getUsername());

            if (message.getUser().getUserRoles() != null && !message.getUser().getUserRoles().isEmpty()) {
                String roleName = message.getUser().getUserRoles().iterator().next().getRole().getRoleName();
                dto.setUserRole(roleName);
            }
        }

        dto.setMessageContent(message.getMessageContent());
        dto.setSentDate(message.getSentDate());
        dto.setIsRead(message.getIsRead());
        dto.setIsAi(message.getIsAi());

        return dto;
    }
}