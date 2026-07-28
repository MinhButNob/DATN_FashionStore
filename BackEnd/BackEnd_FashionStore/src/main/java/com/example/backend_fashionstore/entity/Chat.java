// Chat.java
// VỊ TRÍ: com/example/backend_fashionstore/entity/Chat.java (THAY THẾ file cũ)
package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "chats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private Long chatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    // Giá trị: WAITING (đang chờ, chưa có staff) / ACTIVE (đã gán staff) / CLOSED
    @Column(name = "status")
    private String status;

    // THÊM: phân biệt kênh - "AI" (chat với Haru) hoặc "HUMAN" (chat với nhân viên thật).
    // BẮT BUỘC phải set rõ ràng khi tạo Chat mới ở CẢ 2 nơi (ai_chat và chat_nhan_vien),
    // nếu không 2 luồng sẽ vô tình dùng nhầm chat của nhau (đã từng xảy ra bug thật:
    // tab "Nhân viên tư vấn" hiển thị nhầm tin nhắn của Haru do cả 2 cùng set status=ACTIVE
    // mà không có gì phân biệt).
    @Column(name = "channel")
    private String channel = "HUMAN";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // THÊM: thời điểm được gán cho staff - dùng để FIFO queue và thống kê
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    // THÊM: thời điểm đóng chat
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> messages;
}