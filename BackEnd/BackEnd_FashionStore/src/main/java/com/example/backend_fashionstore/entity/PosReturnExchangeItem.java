package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pos_return_exchange_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosReturnExchangeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_exchange_item_id")
    private Integer returnExchangeItemId;

    @Column(name = "return_exchange_id")
    private Integer returnExchangeId;

    @Column(name = "order_item_id")
    private Integer orderItemId;

    @Column(name = "product_detail_id")
    private Integer productDetailId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_price", precision = 15, scale = 0)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 15, scale = 0)
    private BigDecimal totalAmount;

    @Column(name = "original_total", precision = 15, scale = 0)
    private BigDecimal originalTotal;

    @Column(name = "discount_allocated", precision = 15, scale = 0)
    private BigDecimal discountAllocated;

    @Column(name = "type", length = 20)
    private String type; // RETURN hoặc EXCHANGE

    @Column(name = "new_product_detail_id")
    private Integer newProductDetailId;

    @Column(name = "new_quantity")
    private Integer newQuantity;

    @Column(name = "condition_status", length = 20)
    private String condition; // GOOD hoặc DAMAGED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}