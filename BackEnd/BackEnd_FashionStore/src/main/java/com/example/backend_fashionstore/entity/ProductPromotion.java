package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_promotion_id")
    private Integer productPromotionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_promotion_product_detail_id", nullable = false)
    private ProductDetail productDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_promotion_promotion_id", nullable = false)
    private Promotion promotion;
}