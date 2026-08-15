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

    @Column(name = "shipping_address_address_line")
    private String shippingAddressAddressLine;
    // ===== THÔNG TIN NGƯỜI NHẬN =====
    @Column(name = "shipping_address_full_name")
    private String shippingAddressFullName;

    @Column(name = "shipping_address_phone")
    private String shippingAddressPhone;

    // ===== ĐỊA CHỈ THEO THÔNG TƯ MỚI =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_address_province_id")
    private Province province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_address_ward_id")
    private Ward ward;

    @Column(name = "shipping_address_street", columnDefinition = "TEXT")
    private String streetAddress;

    @Column(name = "shipping_address_province_code", length = 20)
    private String provinceCode;
    @Column(name = "shipping_address_province_name", length = 150)
    private String provinceName;
    @Column(name = "shipping_address_district_code", length = 20)
    private String districtCode;
    @Column(name = "shipping_address_district_name", length = 150)
    private String districtName;
    @Column(name = "shipping_address_ward_code", length = 20)
    private String wardCode;
    @Column(name = "shipping_address_ward_name", length = 150)
    private String wardName;
    @Column(name = "shipping_address_detail", columnDefinition = "TEXT")
    private String addressDetail;

    @Column(name = "shipping_address_needs_update", nullable = false)
    private Boolean needsUpdate = true;

    @Column(name = "shipping_address_full", columnDefinition = "TEXT")
    private String fullAddress;

    // ===== GIỮ LẠI CÁC FIELD CŨ ĐỂ TƯƠNG THÍCH NGƯỢC =====
    @Deprecated
    @Column(name = "shipping_address_city")
    private String shippingAddressCity;

    @Deprecated
    @Column(name = "shipping_address_district")
    private String shippingAddressDistrict;

    @Deprecated
    @Column(name = "shipping_address_ward")
    private String shippingAddressWard;

    // ===== CÁC FIELD KHÁC =====
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
        if (needsUpdate == null) needsUpdate = true;
        buildFullAddress();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (needsUpdate == null) needsUpdate = true;
        buildFullAddress();
    }

    /**
     * Tự động tạo địa chỉ đầy đủ từ các thành phần
     */
    public void buildFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (streetAddress != null && !streetAddress.isEmpty()) {
            sb.append(streetAddress);
        }
        String normalizedWard = wardName != null && !wardName.isBlank() ? wardName : shippingAddressWard;
        String normalizedDistrict = districtName != null && !districtName.isBlank() ? districtName : shippingAddressDistrict;
        String normalizedProvince = provinceName != null && !provinceName.isBlank() ? provinceName : shippingAddressCity;
        String detail = addressDetail != null && !addressDetail.isBlank() ? addressDetail : streetAddress;
        sb.setLength(0);
        if (detail != null && !detail.isEmpty()) {
            sb.append(detail);
        }
        if (normalizedWard != null && !normalizedWard.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(normalizedWard);
        }
        if (normalizedDistrict != null && !normalizedDistrict.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(normalizedDistrict);
        }
        if (normalizedProvince != null && !normalizedProvince.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(normalizedProvince);
        }
        this.fullAddress = sb.toString();
    }

    // ===== GETTERS CHO CÁC FIELD CŨ (Để tương thích) =====
    // Nếu bạn đang gọi getShippingAddressAddressLine() ở đâu đó,
    // hãy thêm method này hoặc đổi tên field cho đúng

    public String getShippingAddressAddressLine() {
        // Trả về streetAddress hoặc shippingAddressAddressLine tùy theo tên cột
        return this.streetAddress;
    }

    // Hoặc nếu field của bạn là shippingAddressAddressLine, hãy đổi tên:
    // @Column(name = "shipping_address_address_line")
    // private String shippingAddressAddressLine;
}
