package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import com.example.backend_fashionstore.enums.OrderItemStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_detail_id")
    private Long orderDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_detail_order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_detail_product_detail_id", nullable = false)
    private ProductDetail productDetail;

    @Column(name = "order_detail_quantity", nullable = false)
    private Integer quantity;

    @Column(name = "order_detail_unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "order_detail_discount", nullable = false)
    private BigDecimal discount;

    @Column(name = "order_detail_final_amount", nullable = false)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_detail_status", nullable = true)
    @Builder.Default
    private OrderItemStatus status = OrderItemStatus.COMPLETED;

    @CreationTimestamp
    @Column(name = "order_detail_created_at", updatable = false)
    private LocalDateTime orderDetailCreatedAt;

    // ===== Các phương thức tiện ích =====

    public void calculateFinalAmount() {
        if (unitPrice != null && quantity != null) {
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            if (discount != null) {
                this.finalAmount = subtotal.subtract(discount);
            } else {
                this.finalAmount = subtotal;
            }
        }
    }
}
