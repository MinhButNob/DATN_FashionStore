// ImageCustomerResponse.java
package com.example.backend_fashionstore.customer.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImageCustomerResponse {
    private Integer productImageId;
    private String imageUrl;
    private Boolean isMain;
    private Integer displayOrder;
}