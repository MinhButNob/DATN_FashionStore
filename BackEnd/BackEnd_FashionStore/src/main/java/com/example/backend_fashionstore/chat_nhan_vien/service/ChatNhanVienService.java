package com.example.backend_fashionstore.chat_nhan_vien.service;

import com.example.backend_fashionstore.auth.repository.CustomerRepository;
import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.chat_nhan_vien.dto.*;
import com.example.backend_fashionstore.chat_nhan_vien.repository.ChatNhanVienRepository;
import com.example.backend_fashionstore.chat_nhan_vien.repository.MessageNhanVienRepository;
import com.example.backend_fashionstore.chat_nhan_vien.websocket.ChatWebSocketHandler;
import com.example.backend_fashionstore.entity.Chat;
import com.example.backend_fashionstore.entity.Customer;
import com.example.backend_fashionstore.entity.Message;
import com.example.backend_fashionstore.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatNhanVienService {

    private final ChatNhanVienRepository chatRepository;
    private final MessageNhanVienRepository messageRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ChatAssignmentService assignmentService;
    private final ChatWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;  // ✅ Spring sẽ tự inject từ JacksonConfig

    // ===== CHAT TYPE CONFIGURATION =====
    private static final Map<String, String> CHAT_TYPE_LABELS = new HashMap<>();
    static {
        CHAT_TYPE_LABELS.put("RETURN", "Hoàn hàng");
        CHAT_TYPE_LABELS.put("PRODUCT_CONSULTATION", "Tư vấn sản phẩm");
        CHAT_TYPE_LABELS.put("RETURN_ONLINE", "Tư vấn hoàn hàng online");
        CHAT_TYPE_LABELS.put("RETURN_STORE", "Tư vấn hoàn hàng tại quầy");
        CHAT_TYPE_LABELS.put("OTHER", "Lý do khác");
    }

    // ===== CREATE CHAT WITH TYPE =====
    public ChatDTO createChat(ChatRequest request) {
        Integer userId = request.getCustomerId().intValue();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Customer customer = customerRepository.findByUser_UserId(userId)
                .orElseGet(() -> {
                    Customer newCustomer = new Customer();
                    newCustomer.setUser(user);
                    newCustomer.setCustomerName(user.getUsername() != null ? user.getUsername() : "Khách hàng " + userId);
                    return customerRepository.save(newCustomer);
                });

        // Kiểm tra chat ACTIVE đang tồn tại
        List<Chat> activeChats = chatRepository.findByCustomer_CustomerIdAndStatusAndChannel(
                customer.getCustomerId(), "ACTIVE", "HUMAN");
        if (!activeChats.isEmpty()) {
            Chat existing = activeChats.get(0);
            return convertToDTO(existing);
        }

        // Kiểm tra chat WAITING
        List<Chat> waitingChats = chatRepository.findByCustomer_CustomerIdAndStatusAndChannel(
                customer.getCustomerId(), "WAITING", "HUMAN");
        if (!waitingChats.isEmpty()) {
            return convertToDTO(waitingChats.get(0));
        }

        // Tạo chat mới với loại chat
        Chat chat = new Chat();
        chat.setCustomer(customer);
        chat.setStatus("WAITING");
        chat.setChannel("HUMAN");
        chat.setChatType(request.getChatType() != null ? request.getChatType() : "OTHER");
        chat.setCreatedAt(LocalDateTime.now());
        chat.setTransferHistory("[]");
        chat = chatRepository.save(chat);

        boolean assigned = assignmentService.tryAssignOne(chat);
        ChatDTO dto = convertToDTO(chat);

        if (!assigned) {
            webSocketHandler.broadcastToAllStaff("queue_updated", Map.of("waitingCount", chatRepository.countWaiting()));
        }

        return dto;
    }

    // ===== TRANSFER CHAT =====
    public ChatDTO transferChat(Long chatId, Long fromUserId, TransferChatRequest request) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!"ACTIVE".equals(chat.getStatus())) {
            throw new RuntimeException("Chỉ có thể chuyển chat đang hoạt động");
        }

        User fromUser = userRepository.findById(fromUserId.intValue())
                .orElseThrow(() -> new RuntimeException("User not found"));

        User toUser = userRepository.findById(request.getToUserId().intValue())
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        // Cập nhật lịch sử chuyển
        String historyJson = chat.getTransferHistory();
        List<TransferHistoryDTO> history = parseTransferHistory(historyJson);

        TransferHistoryDTO transfer = TransferHistoryDTO.builder()
                .fromUser(fromUser.getUsername())
                .toUser(toUser.getUsername())
                .transferredAt(LocalDateTime.now())
                .reason(request.getReason())
                .build();
        history.add(transfer);

        try {
            chat.setTransferHistory(objectMapper.writeValueAsString(history));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot save transfer history");
        }

        chat.setAdmin(toUser);
        chat.setTransferredBy(fromUser.getUsername());
        chat.setTransferredAt(LocalDateTime.now());
        chatRepository.save(chat);

        // Gửi WS thông báo
        ChatDTO dto = convertToDTO(chat);
        Integer customerUserId = chat.getCustomer() != null && chat.getCustomer().getUser() != null
                ? chat.getCustomer().getUser().getUserId() : null;

        webSocketHandler.sendToChatParticipants(customerUserId, request.getToUserId().intValue(), "chat_transferred", dto);

        return dto;
    }

    // ===== GET CHAT TYPES =====
    public List<Map<String, String>> getChatTypes() {
        return CHAT_TYPE_LABELS.entrySet().stream()
                .map(entry -> {
                    Map<String, String> type = new HashMap<>();
                    type.put("value", entry.getKey());
                    type.put("label", entry.getValue());
                    return type;
                })
                .collect(Collectors.toList());
    }

    // ===== SEND MESSAGE =====
    public MessageDTO sendMessage(MessageRequest request) {
        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        Integer userId = request.getUserId().intValue();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isFromCustomer = chat.getCustomer() != null
                && chat.getCustomer().getUser() != null
                && chat.getCustomer().getUser().getUserId().equals(userId);

        if ("WAITING".equals(chat.getStatus()) && isFromCustomer) {
            assignmentService.tryAssignOne(chat);
        }

        if (!isFromCustomer) {
            boolean isAssignedStaff = chat.getAdmin() != null && chat.getAdmin().getUserId().equals(userId);
            if (!isAssignedStaff) {
                throw new RuntimeException("Bạn không phải nhân viên đang phụ trách cuộc trò chuyện này. " +
                        "Hãy bấm \"Nhận chat\" trước khi trả lời.");
            }
        }

        Message message = new Message();
        message.setChat(chat);
        message.setUser(user);
        message.setMessageContent(request.getMessage());
        message.setIsAi(request.getIsAi() != null && request.getIsAi());
        message.setIsRead(false);
        message.setSentDate(LocalDateTime.now());
        Message saved = messageRepository.save(message);
        MessageDTO dto = convertToMessageDTO(saved);

        Integer customerUserId = (chat.getCustomer() != null && chat.getCustomer().getUser() != null)
                ? chat.getCustomer().getUser().getUserId() : null;
        Integer staffUserId = chat.getAdmin() != null ? chat.getAdmin().getUserId() : null;

        webSocketHandler.sendToChatParticipants(customerUserId, staffUserId, "new_message",
                Map.of("chatId", chat.getChatId(), "message", dto));

        return dto;
    }

    // ===== GET CHATS BY STAFF =====
    public List<ChatDTO> getChatsByStaff(Long staffUserId) {
        List<Chat> chats = chatRepository.findByAdmin_UserId(staffUserId.intValue());
        return chats.stream()
                .filter(chat -> "HUMAN".equals(chat.getChannel()))
                .sorted((a, b) -> {
                    int statusOrderA = getStatusOrder(a.getStatus());
                    int statusOrderB = getStatusOrder(b.getStatus());
                    if (statusOrderA != statusOrderB) {
                        return Integer.compare(statusOrderA, statusOrderB);
                    }
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ===== GET ALL ACTIVE CHATS =====
    public List<ChatDTO> getAllActiveChats() {
        return chatRepository.findByStatusAndChannel("ACTIVE", "HUMAN").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ===== GET ALL CHATS FOR ADMIN =====
    public List<ChatDTO> getAllChatsForAdmin() {
        List<Chat> chats = chatRepository.findByChannel("HUMAN");
        return chats.stream()
                .sorted((a, b) -> {
                    int statusOrderA = getStatusOrder(a.getStatus());
                    int statusOrderB = getStatusOrder(b.getStatus());
                    if (statusOrderA != statusOrderB) {
                        return Integer.compare(statusOrderA, statusOrderB);
                    }
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ===== GET CHATS BY STATUS =====
    public List<ChatDTO> getChatsByStatus(String status) {
        return chatRepository.findByStatusAndChannel(status, "HUMAN").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ===== GET CHATS BY TYPE =====
    public List<ChatDTO> getChatsByType(String chatType) {
        return chatRepository.findByChannel("HUMAN").stream()
                .filter(c -> chatType.equals(c.getChatType()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ===== ACCEPT CHAT =====
    public ChatDTO acceptChat(Long chatId, Long staffUserId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        User staff = userRepository.findById(staffUserId.intValue())
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        if (chat.getAdmin() != null && !chat.getAdmin().getUserId().equals(staffUserId.intValue())
                && "ACTIVE".equals(chat.getStatus())) {
            throw new RuntimeException("Cuộc trò chuyện này đang được nhân viên khác (" +
                    chat.getAdmin().getUsername() + ") phụ trách.");
        }

        chat.setAdmin(staff);
        chat.setStatus("ACTIVE");
        chat.setAssignedAt(LocalDateTime.now());
        chatRepository.save(chat);

        ChatDTO dto = convertToDTO(chat);
        Integer customerUserId = chat.getCustomer() != null && chat.getCustomer().getUser() != null
                ? chat.getCustomer().getUser().getUserId() : null;

        webSocketHandler.sendToChatParticipants(customerUserId, staffUserId.intValue(), "chat_assigned", dto);

        return dto;
    }

    // ===== CLOSE CHAT =====
    public ChatDTO closeChat(Long chatId, Long staffUserId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (chat.getAdmin() != null && !chat.getAdmin().getUserId().equals(staffUserId.intValue())) {
            throw new RuntimeException("Bạn không phải nhân viên đang phụ trách cuộc trò chuyện này.");
        }

        chat.setStatus("CLOSED");
        chat.setClosedAt(LocalDateTime.now());
        chatRepository.save(chat);

        assignmentService.tryAssignWaitingChats();

        return convertToDTO(chat);
    }

    // ===== CLOSE CHAT (không cần staffUserId) =====
    public void closeChat(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        chat.setStatus("CLOSED");
        chat.setClosedAt(LocalDateTime.now());
        chatRepository.save(chat);
        assignmentService.tryAssignWaitingChats();
    }

    // ===== REOPEN CHAT =====
    public ChatDTO reopenChat(Long chatId, Long staffUserId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        User staff = userRepository.findById(staffUserId.intValue())
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        chat.setAdmin(staff);
        chat.setStatus("ACTIVE");
        chat.setClosedAt(null);
        chatRepository.save(chat);

        return convertToDTO(chat);
    }

    // ===== GET WAITING COUNT =====
    public Long getWaitingCount() {
        return chatRepository.countWaiting();
    }

    // ===== GET MESSAGES =====
    public List<MessageDTO> getMessagesByChat(Long chatId) {
        return messageRepository.findByChat_ChatIdOrderBySentDateAsc(chatId).stream()
                .map(this::convertToMessageDTO)
                .collect(Collectors.toList());
    }

    // ===== MARK AS READ =====
    public void markMessagesAsRead(Long chatId, Long userId) {
        messageRepository.markAllAsRead(chatId, userId.intValue());
    }

    // ===== COUNT UNREAD =====
    public Long countTotalUnreadMessages(Long customerId) {
        List<Chat> chats = chatRepository.findByCustomer_User_UserIdAndStatusAndChannel(
                customerId.intValue(), "ACTIVE", "HUMAN");
        return chats.stream()
                .filter(c -> c.getCustomer() != null && c.getCustomer().getUser() != null)
                .mapToLong(c -> {
                    Long count = messageRepository.countUnreadMessages(c.getChatId(), c.getCustomer().getUser().getUserId());
                    return count != null ? count : 0L;
                }).sum();
    }

    // ===== GET CHAT BY ID =====
    public ChatDTO getChatById(Long chatId) {
        return chatRepository.findById(chatId)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
    }

    // ===== GET CHATS BY CUSTOMER =====
    public List<ChatDTO> getChatsByCustomer(Long customerId) {
        return chatRepository.findByCustomer_User_UserIdAndChannel(customerId.intValue(), "HUMAN").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ===== HELPER METHODS =====

    private List<TransferHistoryDTO> parseTransferHistory(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<TransferHistoryDTO>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private int getStatusOrder(String status) {
        if ("WAITING".equals(status)) return 0;
        if ("ACTIVE".equals(status)) return 1;
        return 2; // CLOSED
    }

    private ChatDTO convertToDTO(Chat chat) {
        if (chat == null) return null;
        ChatDTO dto = new ChatDTO();
        dto.setChatId(chat.getChatId());

        // Customer info
        if (chat.getCustomer() != null) {
            Customer c = chat.getCustomer();
            dto.setCustomerId(c.getCustomerId().longValue());
            dto.setCustomerName(c.getCustomerName());
            dto.setPhone(c.getPhone());
            dto.setAddress(c.getAddress());
            if (c.getUser() != null) {
                dto.setCustomerEmail(c.getUser().getEmail());
                dto.setCustomerUserId(c.getUser().getUserId().longValue());
                Long unreadCount = messageRepository.countUnreadMessages(chat.getChatId(), c.getUser().getUserId());
                dto.setUnreadCount(unreadCount != null ? unreadCount.intValue() : 0);
            }
        }

        dto.setCreatedAt(chat.getCreatedAt());
        dto.setStatus(chat.getStatus());

        // Admin info
        if (chat.getAdmin() != null) {
            dto.setAdminId(chat.getAdmin().getUserId().longValue());
            dto.setAdminName(chat.getAdmin().getUsername());
        }

        // Chat type
        dto.setChatType(chat.getChatType());
        dto.setChatTypeLabel(CHAT_TYPE_LABELS.getOrDefault(chat.getChatType(), chat.getChatType()));

        // Transfer info
        dto.setTransferredBy(chat.getTransferredBy());
        dto.setTransferredAt(chat.getTransferredAt());
        dto.setTransferHistory(parseTransferHistory(chat.getTransferHistory()));

        // Last message
        List<Message> lastMessages = messageRepository.findLastMessage(chat.getChatId(), PageRequest.of(0, 1));
        if (!lastMessages.isEmpty() && lastMessages.get(0) != null) {
            dto.setLastMessage(convertToMessageDTO(lastMessages.get(0)));
        }

        return dto;
    }

    private MessageDTO convertToMessageDTO(Message message) {
        if (message == null) return null;
        MessageDTO dto = new MessageDTO();
        dto.setMessageId(message.getMessageId());
        if (message.getChat() != null) dto.setChatId(message.getChat().getChatId());
        if (message.getUser() != null) {
            dto.setUserId(message.getUser().getUserId().longValue());
            dto.setUserName(message.getUser().getUsername());
            if (message.getUser().getUserRoles() != null && !message.getUser().getUserRoles().isEmpty()) {
                dto.setUserRole(message.getUser().getUserRoles().iterator().next().getRole().getRoleName());
            }
        }
        dto.setMessageContent(message.getMessageContent());
        dto.setSentDate(message.getSentDate());
        dto.setIsRead(message.getIsRead());
        dto.setIsAi(message.getIsAi());
        return dto;
    }
}