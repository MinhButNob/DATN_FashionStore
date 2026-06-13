package com.example.backend_fashionstore.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wishlists")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wishlist_id")
    private Long wishlistId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "wishlist_name")
    private String wishlistName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_public")
    private Boolean isPublic;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (wishlistName == null) wishlistName = "Default";
        if (isPublic == null) isPublic = false;
    }
}