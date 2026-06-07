package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    private Integer promotionId;

    @Column(name = "promotion_name", nullable = false, length = 20)
    private String promotionName;

    @Column(name = "promotion_description", length = 20)
    private String promotionDescription;

    @Column(name = "promotion_start_date", nullable = false)
    private LocalDateTime promotionStartDate;

    @Column(name = "promotion_end_date", nullable = false)
    private LocalDateTime promotionEndDate;

    @Column(name = "promotion_discount_percent", precision = 10, scale = 2)
    private BigDecimal promotionDiscountPercent;

    @Column(name = "promotion_status", nullable = false, length = 20)
    private String promotionStatus = "ACTIVE";

    @Column(name = "promotion_created_at", nullable = false)
    private LocalDateTime promotionCreatedAt;

    @Column(name = "promotion_updated_at", nullable = false)
    private LocalDateTime promotionUpdatedAt;

    @Column(name = "promotion_deleted_at")
    private LocalDateTime promotionDeletedAt;

    @PrePersist
    public void prePersist() {
        promotionCreatedAt = LocalDateTime.now();
        promotionUpdatedAt = LocalDateTime.now();
        if (promotionDiscountPercent == null) {
            promotionDiscountPercent = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        promotionUpdatedAt = LocalDateTime.now();
    }
}