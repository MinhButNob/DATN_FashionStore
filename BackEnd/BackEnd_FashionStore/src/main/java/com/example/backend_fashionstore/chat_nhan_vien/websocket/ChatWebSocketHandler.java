// ChatWebSocketHandler.java
// VỊ TRÍ: com/example/backend_fashionstore/chat_nhan_vien/websocket/ChatWebSocketHandler.java (THAY THẾ file cũ)
package com.example.backend_fashionstore.chat_nhan_vien.websocket;

import com.example.backend_fashionstore.chat_nhan_vien.service.ChatAssignmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * FE PHẢI gửi 1 tin nhắn "register" ngay sau khi kết nối WS thành công, ví dụ:
 *   { "type": "register", "role": "CUSTOMER", "userId": 123 }
 *   { "type": "register", "role": "STAFF",    "userId": 45  }
 *
 * Nếu không register, session coi như "vô danh" và sẽ KHÔNG nhận được bất kỳ
 * sự kiện chat nào (đây chính là điểm khác biệt lớn nhất so với bản cũ - bản cũ
 * broadcast cho TẤT CẢ session không phân biệt ai là ai).
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // userId -> tập session (1 người có thể mở nhiều tab/thiết bị)
    private final Map<Integer, Set<WebSocketSession>> customerSessions = new ConcurrentHashMap<>();
    private final Map<Integer, Set<WebSocketSession>> staffSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final ChatAssignmentService assignmentService;

    // @Lazy để phá vòng lặp phụ thuộc: Handler cần gọi AssignmentService khi staff connect,
    // AssignmentService cần hỏi Handler ai đang connect -> phụ thuộc vòng nếu không @Lazy.
    public ChatWebSocketHandler(@Lazy ChatAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sendJson(session, Map.of("type", "connection", "message", "Connected, please register"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<?, ?> data = objectMapper.readValue(message.getPayload(), Map.class);
        String type = String.valueOf(data.get("type"));

        switch (type) {
            case "register" -> handleRegister(session, data);
            case "ping" -> sendJson(session, Map.of("type", "pong", "timestamp", System.currentTimeMillis()));
            default -> System.out.println("⚠️ [WS] Không rõ type: " + type);
        }
    }

    private void handleRegister(WebSocketSession session, Map<?, ?> data) throws IOException {
        Object roleObj = data.get("role");
        Object userIdObj = data.get("userId");
        if (roleObj == null || !(userIdObj instanceof Number)) {
            sendJson(session, Map.of("type", "register_error", "message", "Thiếu role hoặc userId"));
            return;
        }

        String role = String.valueOf(roleObj).toUpperCase();
        int userId = ((Number) userIdObj).intValue();

        session.getAttributes().put("role", role);
        session.getAttributes().put("userId", userId);

        if ("STAFF".equals(role)) {
            staffSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
            System.out.println("👤 [WS] Staff online: userId=" + userId + " | tổng staff đang connect=" + staffSessions.size());
            // Staff vừa vào -> có khách nào đang WAITING thì gán ngay, không cần đợi scheduler
            assignmentService.tryAssignWaitingChats();
        } else {
            customerSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
            System.out.println("👤 [WS] Customer connected: userId=" + userId);
        }

        sendJson(session, Map.of("type", "registered", "role", role, "userId", userId));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Object role = session.getAttributes().get("role");
        Object userIdObj = session.getAttributes().get("userId");
        if (userIdObj == null) return;
        int userId = (Integer) userIdObj;

        Map<Integer, Set<WebSocketSession>> targetMap = "STAFF".equals(role) ? staffSessions : customerSessions;
        Set<WebSocketSession> set = targetMap.get(userId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) targetMap.remove(userId);
        }
        System.out.println("🔌 [WS] Disconnected: role=" + role + ", userId=" + userId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("⚠️ [WS] transport error: " + exception.getMessage());
        if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }

    // ================= API cho Service/Controller gọi =================

    /** Gửi cho 1 khách hàng cụ thể (mọi tab đang mở của người đó) */
    public void sendToCustomer(Integer customerUserId, String type, Object data) {
        sendToSet(customerSessions.get(customerUserId), type, data);
    }

    /** Gửi cho 1 staff cụ thể (mọi tab đang mở của người đó) */
    public void sendToStaffUser(Integer staffUserId, String type, Object data) {
        sendToSet(staffSessions.get(staffUserId), type, data);
    }

    /** Gửi đúng cho 2 phía tham gia 1 cuộc chat - dùng cho hầu hết sự kiện chat */
    public void sendToChatParticipants(Integer customerUserId, Integer staffUserId, String type, Object data) {
        if (customerUserId != null) sendToCustomer(customerUserId, type, data);
        if (staffUserId != null) sendToStaffUser(staffUserId, type, data);
    }

    /** Báo cho TOÀN BỘ staff đang connect (vd: có khách mới vào hàng đợi) */
    public void broadcastToAllStaff(String type, Object data) {
        staffSessions.values().forEach(sessions -> sendToSet(sessions, type, data));
    }

    /** Danh sách staff đang có kết nối WS còn sống = "đang sẵn sàng nhận việc" */
    public Set<Integer> getConnectedStaffUserIds() {
        return Collections.unmodifiableSet(staffSessions.keySet());
    }

    private void sendToSet(Set<WebSocketSession> sessions, String type, Object data) {
        if (sessions == null || sessions.isEmpty()) return;
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", type);
            message.put("data", data);
            message.put("timestamp", System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(message);
            for (WebSocketSession s : sessions) {
                if (s.isOpen()) s.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            System.err.println("❌ [WS] Lỗi gửi tin: " + e.getMessage());
        }
    }

    private void sendJson(WebSocketSession session, Object payload) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }
}