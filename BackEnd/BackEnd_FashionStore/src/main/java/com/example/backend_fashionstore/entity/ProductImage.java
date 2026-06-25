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

    @Column(name = "product_image_display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "product_image_is_main", nullable = false)
    private Boolean isMain = false;

    @Column(name = "product_image_status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "product_image_created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "product_image_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "product_image_deleted_at")
    private LocalDateTime deletedAt;

    // ===== Các phương thức tiện ích =====

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsMain() {
        return isMain;
    }

    public void setIsMain(Boolean isMain) {
        this.isMain = isMain;
    }
}