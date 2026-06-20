package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "order_history_order_status")
    private String orderStatus;

    @Column(name = "order_history_note")
    private String note;

    @CreationTimestamp
    @Column(name = "order_history_timestamp", updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "order_history_deleted_at")
    private LocalDateTime deletedAt;
}