package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pos_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pos_order_item_id")
    private Integer posOrderItemId;

    @Column(name = "pos_order_item_order_id", nullable = false)
    private Integer posOrderItemOrderId;

    @Column(name = "pos_order_item_product_detail_id", nullable = false)
    private Integer posOrderItemProductDetailId;

    @Column(name = "pos_order_item_quantity", nullable = false)
    private Integer posOrderItemQuantity;

    @Column(name = "pos_order_item_unit_price", nullable = false)
    private BigDecimal posOrderItemUnitPrice;

    @Column(name = "pos_order_item_discount", nullable = false)
    private BigDecimal posOrderItemDiscount;

    @Column(name = "pos_order_item_total", insertable = false, updatable = false)
    private BigDecimal posOrderItemTotal;

    @Column(name = "pos_order_item_notes")
    private String posOrderItemNotes;

    // ✅ THÊM: Tình trạng sản phẩm khi trả/đổi
    @Column(name = "pos_order_item_condition", length = 20)
    @Builder.Default
    private String condition = "GOOD"; // GOOD, DAMAGED

    @CreationTimestamp
    @Column(name = "pos_order_item_created_at", updatable = false)
    private LocalDateTime posOrderItemCreatedAt;

    @UpdateTimestamp
    @Column(name = "pos_order_item_updated_at")
    private LocalDateTime posOrderItemUpdatedAt;

    @Column(name = "pos_order_item_deleted_at")
    private LocalDateTime posOrderItemDeletedAt;

    // Relationship - không insert/update qua đây
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pos_order_item_product_detail_id", insertable = false, updatable = false)
    private ProductDetail productDetail;
}