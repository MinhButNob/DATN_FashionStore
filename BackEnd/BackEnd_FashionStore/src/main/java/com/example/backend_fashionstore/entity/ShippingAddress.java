package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipping_address_id")
    private Long shippingAddressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_address_customer_id")
    private Customer customer;

    @Column(name = "shipping_address_full_name")
    private String shippingAddressFullName;

    @Column(name = "shipping_address_phone")
    private String shippingAddressPhone;

    @Column(name = "shipping_address_address_line")
    private String shippingAddressAddressLine;

    @Column(name = "shipping_address_city")
    private String shippingAddressCity;

    @Column(name = "shipping_address_district")
    private String shippingAddressDistrict;

    @Column(name = "shipping_address_ward")
    private String shippingAddressWard;

    @Column(name = "shipping_address_postal_code")
    private String shippingAddressPostalCode;

    @Column(name = "shipping_address_is_default")
    private Boolean isDefault;

    @Column(name = "shipping_address_status")
    private String status;

    @Column(name = "shipping_address_created_at")
    private LocalDateTime createdAt;

    @Column(name = "shipping_address_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "shipping_address_deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
        if (isDefault == null) isDefault = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}