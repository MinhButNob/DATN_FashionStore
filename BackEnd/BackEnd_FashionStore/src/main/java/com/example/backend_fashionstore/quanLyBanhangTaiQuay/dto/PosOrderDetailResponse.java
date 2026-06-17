package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.util.List;

@Data
public class PosOrderDetailResponse {
    private PosOrderResponse order;
    private List<PosOrderItemDetailResponse> items;
}