package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "provinces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Province {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "province_id")
    private Integer provinceId;

    @Column(name = "province_name", nullable = false, length = 100)
    private String provinceName;

    @Column(name = "province_code", nullable = false, unique = true, length = 20)
    private String provinceCode;

    @Column(name = "province_type", length = 50)
    private String provinceType;  // 'Thành phố', 'Tỉnh'

    @Column(name = "province_status")
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "province_display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "province_created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "province_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "province_deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "province", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Ward> wards = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "ACTIVE";
        if (displayOrder == null) displayOrder = 0;
    }
}