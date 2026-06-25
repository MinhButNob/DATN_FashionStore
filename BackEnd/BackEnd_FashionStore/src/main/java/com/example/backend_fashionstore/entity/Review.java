package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer reviewId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_detail_id", nullable = false)
    private Integer productDetailId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "rating", nullable = false)
    private Integer rating;  // 1-5

    @Column(name = "comment")
    private String comment;

    @Column(name = "review_status")
    private String reviewStatus;  // APPROVED, PENDING, REJECTED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (reviewStatus == null) {
            reviewStatus = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}