package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "banners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banner_id")
    private Integer bannerId;

    @Column(name = "banner_title", nullable = false, length = 255)
    private String title;

    @Column(name = "banner_subtitle", length = 255)
    private String subtitle;

    // Đường dẫn ảnh banner (lưu local hoặc URL)
    @Column(name = "banner_image_path", nullable = false)
    private String imagePath;

    // Loại link khi click vào banner: CATEGORY | PRODUCT | URL
    @Column(name = "banner_link_type", nullable = false, length = 20)
    private String linkType; // "CATEGORY", "PRODUCT", "URL"

    // ID danh mục (nếu linkType = CATEGORY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_category_id")
    private Category category;

    // ID sản phẩm (nếu linkType = PRODUCT)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_product_id")
    private Product product;

    // URL tùy chỉnh (nếu linkType = URL)
    @Column(name = "banner_custom_url", length = 500)
    private String customUrl;

    // Thứ tự hiển thị (sort order)
    @Column(name = "banner_display_order")
    private Integer displayOrder;

    // Trạng thái: ACTIVE, INACTIVE, DELETED
    @Column(name = "banner_status", nullable = false, length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "banner_created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "banner_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "banner_deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        if (status == null) status = "ACTIVE";
        if (linkType == null) linkType = "URL";
        if (displayOrder == null) displayOrder = 0;
    }
}