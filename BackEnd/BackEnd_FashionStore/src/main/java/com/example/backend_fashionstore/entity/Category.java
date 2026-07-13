package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category_name", nullable = false, length = 20)
    private String categoryName;

    @Column(name = "category_description", length = 20)
    private String categoryDescription;

    @Column(name = "category_status", nullable = false, length = 20)
    private String status;

    // Danh mục cha (dùng cho tính năng Phân cấp danh mục 2 cấp).
    // null = danh mục cấp 1 (không có cha) | có giá trị = là con của danh mục đó
    @Column(name = "parent_category_id")
    private Integer parentCategoryId;

    @CreationTimestamp
    @Column(name = "category_created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "category_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "category_deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        if (status == null) status = "ACTIVE";
    }
}