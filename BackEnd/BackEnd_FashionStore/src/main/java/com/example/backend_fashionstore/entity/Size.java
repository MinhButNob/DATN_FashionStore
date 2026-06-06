package com.example.backend_fashionstore.entity;

import com.example.backend_fashionstore.enums.SizeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sizes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Size {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "size_id")
    private Integer sizeId;

    @Column(name = "size_code", nullable = false, unique = true)
    private String sizeCode;

    @Column(name = "size_name", nullable = false)
    private String sizeName;

    @Column(name = "size_description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "size_status", nullable = false, length = 20)
    private SizeStatus status = SizeStatus.ACTIVE;

    @Column(name = "size_created_at")
    private LocalDateTime createdAt;

    @Column(name = "size_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "size_deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = SizeStatus.ACTIVE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}