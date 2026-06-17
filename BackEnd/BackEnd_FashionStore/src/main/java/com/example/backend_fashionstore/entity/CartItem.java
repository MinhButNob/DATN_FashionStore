package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Integer cartItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_item_cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_item_product_detail_id", nullable = false)
    private ProductDetail productDetail;

    @Column(name = "cart_item_quantity", nullable = false)
    private Integer quantity;

    @Column(name = "subtotal")  // hoặc "cart_item_subtotal" nếu đã đổi tên
    private BigDecimal subtotal;

    @Column(name = "cart_item_created_at")
    private LocalDateTime createdAt;

    @Column(name = "cart_item_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cart_item_deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.quantity == null) {
            this.quantity = 1;
        }
        calculateSubtotal();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateSubtotal();
    }

    public void calculateSubtotal() {
        if (this.productDetail != null && this.productDetail.getPrice() != null && this.quantity != null) {
            this.subtotal = this.productDetail.getPrice().multiply(BigDecimal.valueOf(this.quantity));
        } else {
            this.subtotal = BigDecimal.ZERO;
        }
    }
}