package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "colors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Color {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "color_id")
    private Integer colorId;

    @Column(name = "color_code", nullable = false, length = 20)
    private String colorCode;

    @Column(name = "color_name", nullable = false, length = 20)
    private String colorName;

    @Column(name = "color_hex_code", length = 20)
    private String hexCode;

    @Column(name = "color_status", nullable = false, length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "color_created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "color_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "color_deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        if (status == null) status = "ACTIVE";
    }
}