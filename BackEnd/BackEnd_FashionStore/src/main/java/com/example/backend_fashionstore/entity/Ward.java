package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ward_id")
    private Integer wardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_province_id", nullable = false)
    @ToString.Exclude
    private Province province;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "ward_name", nullable = false, length = 100)
    private String wardName;

    @Column(name = "ward_code", nullable = false, unique = true, length = 20)
    private String wardCode;

    @Column(name = "ward_type", length = 50)
    private String wardType;  // 'Phường', 'Xã', 'Thị trấn'

    @Column(name = "ward_status")
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "ward_display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "ward_created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "ward_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "ward_deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "ACTIVE";
        if (displayOrder == null) displayOrder = 0;
    }
}