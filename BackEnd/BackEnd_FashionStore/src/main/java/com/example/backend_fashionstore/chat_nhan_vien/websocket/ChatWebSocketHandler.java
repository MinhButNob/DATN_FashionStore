package com.example.backend_fashionstore.chat_nhan_vien.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentSkipListSet<Long> broadcastedMessageIds = new ConcurrentSkipListSet<>();
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        System.out.println("🔗 WebSocket connected: " + sessionId);
        System.out.println("📊 Total sessions: " + sessions.size());

        Map<String, Object> response = new HashMap<>();
        response.put("type", "connection");
        response.put("message", "Connected successfully");
        response.put("sessionId", sessionId);

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("📨 Received message: " + payload);

        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = (String) data.get("type");

            if ("new_message".equals(type)) {
                Map<String, Object> msgData = (Map<String, Object>) data.get("data");
                broadcastToStaff("new_message", msgData);
            } else if ("ping".equals(type)) {
                Map<String, Object> pong = new HashMap<>();
                pong.put("type", "pong");
                pong.put("timestamp", System.currentTimeMillis());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        System.out.println("🔌 WebSocket disconnected: " + sessionId);
        System.out.println("📊 Total sessions: " + sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("⚠️ WebSocket transport error: " + exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    // ===== SỬA: broadcastToStaff - Kiểm tra trùng lặp =====
    public void broadcastToStaff(String type, Object data) {
        try {
            Long messageId = extractMessageId(data);
            if (messageId != null && broadcastedMessageIds.contains(messageId)) {
                System.out.println("⏭️ Duplicate broadcast skipped: " + messageId);
                return;
            }

            if (messageId != null) {
                broadcastedMessageIds.add(messageId);
                if (broadcastedMessageIds.size() > 10000) {
                    broadcastedMessageIds.pollFirst();
                }
            }

            Map<String, Object> message = new HashMap<>();
            message.put("type", type);
            message.put("data", data);
            message.put("timestamp", System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(message);

            int count = 0;
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                    count++;
                }
            }
            System.out.println("📤 Broadcasted to " + count + " sessions");
        } catch (IOException e) {
            System.err.println("Error broadcasting: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Long extractMessageId(Object data) {
        try {
            if (data instanceof Map) {
                Map<?, ?> dataMap = (Map<?, ?>) data;
                Object messageObj = dataMap.get("message");
                if (messageObj instanceof Map) {
                    Map<?, ?> messageMap = (Map<?, ?>) messageObj;
                    Object messageIdObj = messageMap.get("messageId");
                    if (messageIdObj instanceof Number) {
                        return ((Number) messageIdObj).longValue();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    public void notifyStaffNewMessage(Map<String, Object> messageData) {
        broadcastToStaff("new_message", messageData);
    }
}