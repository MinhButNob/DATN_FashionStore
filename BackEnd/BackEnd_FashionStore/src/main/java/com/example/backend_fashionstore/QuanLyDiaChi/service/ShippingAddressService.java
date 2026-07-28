package com.example.backend_fashionstore.QuanLyDiaChi.service;

import com.example.backend_fashionstore.QuanLyDiaChi.dto.request.ShippingAddressRequestDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.ShippingAddressResponseDTO;

import java.util.List;

public interface ShippingAddressService {

    List<ShippingAddressResponseDTO> getAddressesByCustomer(Integer customerId);

    ShippingAddressResponseDTO getAddressById(Long addressId, Integer customerId);

    ShippingAddressResponseDTO createAddress(Integer customerId, ShippingAddressRequestDTO request);

    ShippingAddressResponseDTO updateAddress(Long addressId, Integer customerId, ShippingAddressRequestDTO request);

    void setDefaultAddress(Long addressId, Integer customerId);

    void deleteAddress(Long addressId, Integer customerId);

    // ✅ THÊM PHƯƠNG THỨC NÀY
    ShippingAddressResponseDTO getDefaultAddress(Integer customerId);
}