package com.example.backend_fashionstore.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"wishlist_item_wishlist_id", "wishlist_item_product_detail_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wishlist_item_id")
    private Long wishlistItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wishlist_item_wishlist_id", nullable = false)
    private Wishlist wishlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wishlist_item_product_detail_id", nullable = false)
    private ProductDetail productDetail;

    @Column(name = "wishlist_item_added_at", nullable = false, updatable = false)
    private LocalDateTime wishlistItemAddedAt;

    @Column(name = "wishlist_item_notes")
    private String wishlistItemNotes;

    @Column(name = "wishlist_item_status", nullable = false, length = 20)
    private String wishlistItemStatus;

    @Column(name = "wishlist_item_created_at", nullable = false, updatable = false)
    private LocalDateTime wishlistItemCreatedAt;

    @Column(name = "wishlist_item_updated_at", nullable = false)
    private LocalDateTime wishlistItemUpdatedAt;

    @Column(name = "wishlist_item_deleted_at")
    private LocalDateTime wishlistItemDeletedAt;

    @PrePersist
    protected void onCreate() {
        wishlistItemAddedAt = LocalDateTime.now();
        wishlistItemCreatedAt = LocalDateTime.now();
        wishlistItemUpdatedAt = LocalDateTime.now();
        if (wishlistItemStatus == null) wishlistItemStatus = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        wishlistItemUpdatedAt = LocalDateTime.now();
    }
}