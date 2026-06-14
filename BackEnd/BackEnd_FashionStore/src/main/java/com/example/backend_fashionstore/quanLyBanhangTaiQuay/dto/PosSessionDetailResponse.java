package com.example.backend_fashionstore.quanLyBanhangTaiQuay.dto;

import lombok.Data;
import java.util.List;

@Data
public class PosSessionDetailResponse {
    private PosSessionResponse session;
    private List<PosOrderSimpleResponse> orders;
    private List<PosCashTransactionResponse> cashTransactions;
}