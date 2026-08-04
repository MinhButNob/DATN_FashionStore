package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(name = "status")
    private String status; // WAITING, ACTIVE, CLOSED

    @Column(name = "channel")
    private String channel = "HUMAN";

    @Column(name = "chat_type")
    private String chatType; // RETURN, PRODUCT_CONSULTATION, RETURN_ONLINE, RETURN_STORE, OTHER

    @Column(name = "transferred_by")
    private String transferredBy;

    @Column(name = "transferred_at")
    private LocalDateTime transferredAt;

    @Column(name = "transfer_history", columnDefinition = "TEXT")
    private String transferHistory;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ✅ THÊM: updatedAt
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> messages;
}