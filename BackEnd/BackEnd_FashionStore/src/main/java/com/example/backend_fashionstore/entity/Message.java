// Message.java
package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "message_content")
    private String messageContent;

    @CreationTimestamp
    @Column(name = "sent_date", updatable = false)
    private LocalDateTime sentDate;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "is_ai")
    private Boolean isAi = false;
}