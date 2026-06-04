package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Integer customerId;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    private String phone;

    private String address;

    @Column(name = "customer_status")
    private String customerStatus = "ACTIVE";  // ACTIVE, LOCKED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (customerStatus == null) {
            customerStatus = "ACTIVE";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods để tương thích với code cũ
    public boolean isLocked() {
        return "LOCKED".equals(customerStatus);
    }

    public void setLocked(boolean locked) {
        this.customerStatus = locked ? "LOCKED" : "ACTIVE";
    }
}