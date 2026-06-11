package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_detail_id")
    private Integer productDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_detail_product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_detail_color_id", nullable = false)
    private Color color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_detail_size_id", nullable = false)
    private Size size;

    @Column(name = "product_detail_sku", nullable = false, length = 20)
    private String sku;

    // ✅ Dùng đúng tên cột trong database
    @Column(name = "product_detail_price", nullable = false)
    private BigDecimal price;

    @Column(name = "product_detail_quantity", nullable = false)
    private Integer quantity;

    @Column(name = "product_detail_status", nullable = false, length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "product_detail_created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "product_detail_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "product_detail_deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "productDetail", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductImage> productImages = new ArrayList<>();

    // ⚠️ IGNORE các cột cũ - KHÔNG map vào entity
    // Không khai báo các field: price_old, created_at_old, updated_at_old, v.v.

    @PrePersist
    public void prePersist() {
        if (status == null) status = "ACTIVE";
        if (quantity == null) quantity = 0;
    }
}