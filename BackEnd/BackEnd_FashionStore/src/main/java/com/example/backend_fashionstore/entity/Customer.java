package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Integer customerId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "phone", unique = true, nullable = false) // ✅ THÊM unique = true
    private String phone;

    // ===== ĐỊA CHỈ THEO THÔNG TƯ MỚI =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_province_id")
    private Province province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_ward_id")
    private Ward ward;

    @Column(name = "customer_street_address", columnDefinition = "TEXT")
    private String streetAddress;

    // ===== GIỮ LẠI FIELD CŨ ĐỂ TƯƠNG THÍCH =====
    @Deprecated
    @Column(name = "address")
    private String address;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;



    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Lấy địa chỉ đầy đủ
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (streetAddress != null && !streetAddress.isEmpty()) {
            sb.append(streetAddress);
        }
        if (ward != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ward.getWardName());
        }
        if (province != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(province.getProvinceName());
        }
        return sb.toString();
    }


}