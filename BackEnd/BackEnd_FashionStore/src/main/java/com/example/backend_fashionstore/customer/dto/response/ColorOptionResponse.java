// ColorOptionResponse.java
package com.example.backend_fashionstore.customer.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ColorOptionResponse {
    private Integer colorId;
    private String colorName;
    private String colorCode;
    private String hexCode;
}