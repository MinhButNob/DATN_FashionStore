package com.example.backend_fashionstore.shipping.service;

import com.example.backend_fashionstore.shipping.dto.ShippingCalculateRequest;
import com.example.backend_fashionstore.shipping.dto.ShippingCalculateResponse;

public interface ShippingService {
    ShippingCalculateResponse calculate(Integer customerId, ShippingCalculateRequest request);
}
