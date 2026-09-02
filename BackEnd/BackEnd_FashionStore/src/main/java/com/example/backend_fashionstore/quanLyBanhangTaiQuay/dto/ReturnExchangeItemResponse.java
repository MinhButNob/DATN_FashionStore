package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ReturnExchangeItemResponse {
    private Integer itemId;
    private String productName;
    private String colorName;
    private String sizeName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;        // Số tiền thực tế hoàn (đã trừ giảm giá)
    private BigDecimal originalTotal;     // Giá gốc chưa giảm giá
    private BigDecimal discountAllocated; // Giảm giá phân bổ
    private String type;                  // RETURN hoặc EXCHANGE
    private String newProductName;        // Tên sản phẩm mới (khi đổi)
    private Integer newQuantity;          // Số lượng sản phẩm mới
    private String condition;             // GOOD hoặc DAMAGED
    private Integer productDetailId;      // ✅ Thêm ID chi tiết sản phẩm
    private Integer orderItemId;          // ✅ Thêm ID item trong đơn hàng
}