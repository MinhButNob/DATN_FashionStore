package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.util.List;

@Data
public class PosReturnExchangeRequest {
    private Integer orderId;
    private String customerName;
    private String customerPhone;
    private String type;
    private String reason;
    private String notes;
    private List<ReturnItemRequest> returnItems;
    private List<ExchangeItemRequest> exchangeItems;
}