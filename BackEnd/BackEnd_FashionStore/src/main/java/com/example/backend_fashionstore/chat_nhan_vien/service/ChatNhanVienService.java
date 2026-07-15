// ChatNhanVienService.java
// VỊ TRÍ: com/example/backend_fashionstore/chat_nhan_vien/service/ChatNhanVienService.java (THAY THẾ file cũ)
package com.example.backend_fashionstore.chat_nhan_vien.service;

import com.example.backend_fashionstore.auth.repository.CustomerRepository;
import com.example.backend_fashionstore.auth.repository.UserRepository;
import com.example.backend_fashionstore.chat_nhan_vien.dto.ChatDTO;
import com.example.backend_fashionstore.chat_nhan_vien.dto.ChatRequest;
import com.example.backend_fashionstore.chat_nhan_vien.dto.MessageDTO;
import com.example.backend_fashionstore.chat_nhan_vien.dto.MessageRequest;
import com.example.backend_fashionstore.chat_nhan_vien.repository.ChatNhanVienRepository;
import com.example.backend_fashionstore.chat_nhan_vien.repository.MessageNhanVienRepository;
import com.example.backend_fashionstore.chat_nhan_vien.websocket.ChatWebSocketHandler;
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
import java.util.Map;
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

    // Yêu cầu chat mới (hoặc lấy lại chat ACTIVE/WAITING sẵn có của khách)
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

        // FIX: findByCustomer_CustomerIdAndStatus() cũ không lọc channel - có thể
        // nhặt trúng chat ACTIVE của Haru (channel="AI") nếu khách đang chat với AI.
        // Dùng bản có filter channel="HUMAN" để đảm bảo chỉ lấy đúng chat nhân viên.
        List<Chat> activeChats = chatRepository.findByCustomer_CustomerIdAndStatusAndChannel(
                customer.getCustomerId(), "ACTIVE", "HUMAN");
        if (!activeChats.isEmpty()) return convertToDTO(activeChats.get(0));

        List<Chat> waitingChats = chatRepository.findByCustomer_CustomerIdAndStatusAndChannel(
                customer.getCustomerId(), "WAITING", "HUMAN");
        if (!waitingChats.isEmpty()) return convertToDTO(waitingChats.get(0));

        Chat chat = new Chat();
        chat.setCustomer(customer);
        chat.setStatus("WAITING");
        chat.setChannel("HUMAN"); // FIX: đánh dấu rõ đây là chat với nhân viên thật
        chat.setCreatedAt(LocalDateTime.now());
        chat = chatRepository.save(chat);

        boolean assigned = assignmentService.tryAssignOne(chat);
        ChatDTO dto = convertToDTO(chat);

        if (assigned) {
            webSocketHandler.sendToChatParticipants(userId, chat.getAdmin().getUserId(), "chat_assigned", dto);
        } else {
            webSocketHandler.broadcastToAllStaff("queue_updated", Map.of("waitingCount", chatRepository.countWaiting()));
        }

        return dto;
    }

    // ===== TẬP TRUNG WS-SEND TẠI ĐÂY -> mọi endpoint gọi sendMessage đều tự động
    // bắn realtime đúng cho cả khách + staff, không còn sót controller nào quên gửi WS =====
    public MessageDTO sendMessage(MessageRequest request) {
        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        Integer userId = request.getUserId().intValue();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isFromCustomer = chat.getCustomer() != null
                && chat.getCustomer().getUser() != null
                && chat.getCustomer().getUser().getUserId().equals(userId);

        // Khách nhắn khi chat còn WAITING -> thử gán staff lại (phòng trường hợp staff
        // vừa mới online sau khi khách đã tạo chat)
        if ("WAITING".equals(chat.getStatus()) && isFromCustomer) {
            assignmentService.tryAssignOne(chat);
        }

        // FIX: nếu người gửi KHÔNG phải khách hàng (tức là staff), bắt buộc phải là
        // đúng staff đang được gán (chat.admin) mới được gửi tin. Trước đây không có
        // check này -> BẤT KỲ staff nào biết chatId cũng gửi được vào, dẫn tới bug
        // "2 nhân viên cùng trả lời 1 chat như group chat".
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

    public ChatDTO getChatById(Long chatId) {
        return chatRepository.findById(chatId).map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
    }

    // LƯU Ý: tham số tên là "customerId" để giữ tương thích chữ ký cũ, nhưng giá trị
    // thực chất FE luôn gửi vào là user.userId (giống hệt các endpoint khác như
    // createChat/sendMessage) - đã fix dùng đúng query theo user.userId bên dưới.
    // FIX thêm: lọc channel="HUMAN" - trước đây liệt kê TẤT CẢ chat của khách (kể cả
    // lịch sử chat với Haru), khiến tab "Nhân viên tư vấn" có thể hiện nhầm hội thoại AI.
    public List<ChatDTO> getChatsByCustomer(Long customerId) {
        return chatRepository.findByCustomer_User_UserIdAndChannel(customerId.intValue(), "HUMAN").stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    // FIX: lọc channel="HUMAN" - tránh chat của Haru lọt vào danh sách này (dùng cho
    // dashboard admin/staff xem toàn bộ hội thoại đang ACTIVE)
    public List<ChatDTO> getAllActiveChats() {
        return chatRepository.findByStatusAndChannel("ACTIVE", "HUMAN").stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<ChatDTO> getChatsByStaff(Long staffUserId) {
        return chatRepository.findByAdmin_UserIdAndStatus(staffUserId.intValue(), "ACTIVE").stream()
                .map(this::convertToDTO).collect(Collectors.toList());
    }

    public Long getWaitingCount() {
        return chatRepository.countWaiting();
    }

    public ChatDTO acceptChat(Long chatId, Long staffUserId) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new RuntimeException("Chat not found"));
        User staff = userRepository.findById(staffUserId.intValue()).orElseThrow(() -> new RuntimeException("Staff not found"));

        // FIX: chặn "cướp" chat đang có staff khác xử lý - trước đây bất kỳ staff nào
        // cũng accept được chat đã ACTIVE của người khác, ghi đè admin.
        if (chat.getAdmin() != null && !chat.getAdmin().getUserId().equals(staffUserId.intValue())
                && "ACTIVE".equals(chat.getStatus())) {
            throw new RuntimeException("Cuộc trò chuyện này đang được nhân viên khác (" +
                    chat.getAdmin().getUsername() + ") phụ trách.");
        }

        chat.setAdmin(staff);
        chat.setStatus("ACTIVE");
        chat.setAssignedAt(LocalDateTime.now());
        return convertToDTO(chatRepository.save(chat));
    }

    public ChatDTO reopenChat(Long chatId, Long staffUserId) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new RuntimeException("Chat not found"));
        User staff = userRepository.findById(staffUserId.intValue()).orElseThrow(() -> new RuntimeException("Staff not found"));
        chat.setAdmin(staff);
        chat.setStatus("ACTIVE");
        return convertToDTO(chatRepository.save(chat));
    }

    public ChatDTO closeChat(Long chatId, Long staffUserId) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new RuntimeException("Chat not found"));

        // FIX: chỉ đúng staff đang phụ trách mới được đóng chat - trước đây staff bất
        // kỳ cũng đóng được chat của người khác.
        if (chat.getAdmin() != null && !chat.getAdmin().getUserId().equals(staffUserId.intValue())) {
            throw new RuntimeException("Bạn không phải nhân viên đang phụ trách cuộc trò chuyện này.");
        }

        chat.setStatus("CLOSED");
        chat.setClosedAt(LocalDateTime.now());
        chatRepository.save(chat);

        // Vừa rảnh 1 slot -> thử gán ngay cho khách đang chờ đầu hàng đợi
        assignmentService.tryAssignWaitingChats();

        return convertToDTO(chat);
    }

    public void closeChat(Long chatId) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new RuntimeException("Chat not found"));
        chat.setStatus("CLOSED");
        chat.setClosedAt(LocalDateTime.now());
        chatRepository.save(chat);
        assignmentService.tryAssignWaitingChats();
    }

    public List<MessageDTO> getMessagesByChat(Long chatId) {
        return messageRepository.findByChat_ChatIdOrderBySentDateAsc(chatId).stream()
                .map(this::convertToMessageDTO).collect(Collectors.toList());
    }

    public void markMessagesAsRead(Long chatId, Long userId) {
        messageRepository.markAllAsRead(chatId, userId.intValue());
    }

    public Long countTotalUnreadMessages(Long customerId) {
        // FIX: tương tự getChatsByCustomer - param thực chất là user.userId, và cần
        // lọc channel="HUMAN" để không tính luôn tin nhắn chưa đọc của Haru vào đây.
        List<Chat> chats = chatRepository.findByCustomer_User_UserIdAndStatusAndChannel(
                customerId.intValue(), "ACTIVE", "HUMAN");
        return chats.stream()
                .filter(c -> c.getCustomer() != null && c.getCustomer().getUser() != null)
                .mapToLong(c -> {
                    Long count = messageRepository.countUnreadMessages(c.getChatId(), c.getCustomer().getUser().getUserId());
                    return count != null ? count : 0L;
                }).sum();
    }

    private ChatDTO convertToDTO(Chat chat) {
        if (chat == null) return null;
        ChatDTO dto = new ChatDTO();
        dto.setChatId(chat.getChatId());
        if (chat.getCustomer() != null) {
            Customer c = chat.getCustomer();
            dto.setCustomerId(c.getCustomerId().longValue());
            dto.setCustomerName(c.getCustomerName());
            dto.setPhone(c.getPhone());
            dto.setAddress(c.getAddress());
            // FIX: guard null đầy đủ trước khi dùng c.getUser() lần 2 (bản cũ NPE ở đây)
            if (c.getUser() != null) {
                dto.setCustomerEmail(c.getUser().getEmail());
                dto.setCustomerUserId(c.getUser().getUserId().longValue());
                Long unreadCount = messageRepository.countUnreadMessages(chat.getChatId(), c.getUser().getUserId());
                dto.setUnreadCount(unreadCount != null ? unreadCount.intValue() : 0);
            } else {
                dto.setUnreadCount(0);
            }
        } else {
            dto.setUnreadCount(0);
        }
        dto.setCreatedAt(chat.getCreatedAt());
        dto.setStatus(chat.getStatus());
        if (chat.getAdmin() != null) {
            dto.setAdminId(chat.getAdmin().getUserId().longValue());
            dto.setAdminName(chat.getAdmin().getUsername());
        }
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