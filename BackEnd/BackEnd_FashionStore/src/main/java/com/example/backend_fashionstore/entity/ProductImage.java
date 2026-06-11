package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_image_id")
    private Integer productImageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_image_product_detail_id", nullable = false)
    private ProductDetail productDetail;

    @Column(name = "product_image_path", nullable = false, length = 500)
    private String productImagePath;

    @Column(name = "product_image_type", length = 50)
    private String productImageType;

    @Column(name = "product_image_display_order")
    private Integer displayOrder;

    @Column(name = "product_image_is_main")
    private Boolean isMain;

    @Column(name = "product_image_status", nullable = false, length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "product_image_created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "product_image_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "product_image_deleted_at")
    private LocalDateTime deletedAt;

    // ========== CÁC CỘT CŨ (PHẢI CÓ GIÁ TRỊ) ==========
    @Column(name = "created_at")
    private LocalDateTime oldCreatedAt;

    @Column(name = "display_order")
    private Integer oldDisplayOrder;

    @Column(name = "is_main")
    private Boolean oldIsMain;

    @PrePersist
    public void prePersist() {
        if (displayOrder == null) displayOrder = 0;
        if (isMain == null) isMain = false;
        if (status == null) status = "ACTIVE";

        // ✅ Set giá trị cho các cột cũ
        if (oldDisplayOrder == null) {
            oldDisplayOrder = displayOrder;
        }
        if (oldIsMain == null) {
            oldIsMain = isMain;
        }
        if (oldCreatedAt == null) {
            oldCreatedAt = LocalDateTime.now();
        }
    }
}