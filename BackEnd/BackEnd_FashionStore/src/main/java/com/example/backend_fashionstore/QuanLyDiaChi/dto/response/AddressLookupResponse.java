package com.example.backend_fashionstore.QuanLyDiaChi.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressLookupResponse {
    private String code;
    private String name;
}
