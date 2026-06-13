// SizeOptionResponse.java
package com.example.backend_fashionstore.customer.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SizeOptionResponse {
    private Integer sizeId;
    private String sizeName;
    private String sizeCode;
}