package com.example.backend_fashionstore.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "return_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_request_id")
    private Long returnRequestId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    // ✅ THÊM: Danh sách productDetailIds (lưu dạng JSON)
    @Column(name = "product_detail_ids", columnDefinition = "TEXT")
    private String productDetailIds; // JSON array: [1, 2, 3]

    // ✅ THÊM: Số lượng hoàn trả cho từng sản phẩm (lưu dạng JSON)
    @Column(name = "quantities", columnDefinition = "TEXT")
    private String quantities; // JSON object: {"1": 2, "2": 1}

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "images", columnDefinition = "TEXT")
    private String images;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "inspection_result", columnDefinition = "TEXT")
    private String inspectionResult;

    @Column(name = "inspection_note", columnDefinition = "TEXT")
    private String inspectionNote;

    @Column(name = "inspected_by")
    private Integer inspectedBy;

    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
