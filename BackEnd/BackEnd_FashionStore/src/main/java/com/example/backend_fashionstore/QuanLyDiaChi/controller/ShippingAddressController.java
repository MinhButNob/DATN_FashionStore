package com.example.backend_fashionstore.QuanLyDiaChi.controller;

import com.example.backend_fashionstore.QuanLyDiaChi.dto.request.ShippingAddressRequestDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.ShippingAddressResponseDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.service.ShippingAddressService;
import com.example.backend_fashionstore.auth.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/shipping-addresses")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ShippingAddressController {

    private final ShippingAddressService shippingAddressService;
    private final CustomerRepository customerRepository;

    /**
     * Lấy danh sách địa chỉ của khách hàng
     * GET /api/shipping-addresses
     */
    @GetMapping
    public ResponseEntity<List<ShippingAddressResponseDTO>> getMyAddresses(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        log.info("📋 Getting shipping addresses for customer: {}", customerId);
        return ResponseEntity.ok(shippingAddressService.getAddressesByCustomer(customerId));
    }

    /**
     * Lấy chi tiết địa chỉ
     * GET /api/shipping-addresses/{addressId}
     */
    @GetMapping("/{addressId}")
    public ResponseEntity<ShippingAddressResponseDTO> getAddressById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long addressId
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        log.info("📋 Getting shipping address: {} for customer: {}", addressId, customerId);
        return ResponseEntity.ok(shippingAddressService.getAddressById(addressId, customerId));
    }

    /**
     * Lấy địa chỉ mặc định của khách hàng
     * GET /api/shipping-addresses/default
     */
    @GetMapping("/default")
    public ResponseEntity<ShippingAddressResponseDTO> getDefaultAddress(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        log.info("📋 Getting default shipping address for customer: {}", customerId);
        ShippingAddressResponseDTO address = shippingAddressService.getDefaultAddress(customerId);
        return ResponseEntity.ok(address);
    }

    /**
     * Tạo địa chỉ mới
     * POST /api/shipping-addresses
     */
    @PostMapping
    public ResponseEntity<ShippingAddressResponseDTO> createAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ShippingAddressRequestDTO request
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        log.info("➕ Creating shipping address for customer: {}", customerId);
        return ResponseEntity.ok(shippingAddressService.createAddress(customerId, request));
    }

    /**
     * Cập nhật địa chỉ
     * PUT /api/shipping-addresses/{addressId}
     */
    @PutMapping("/{addressId}")
    public ResponseEntity<ShippingAddressResponseDTO> updateAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long addressId,
            @RequestBody ShippingAddressRequestDTO request
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        log.info("✏️ Updating shipping address: {} for customer: {}", addressId, customerId);
        return ResponseEntity.ok(shippingAddressService.updateAddress(addressId, customerId, request));
    }

    /**
     * Đặt địa chỉ mặc định
     * PUT /api/shipping-addresses/{addressId}/default
     */
    @PutMapping("/{addressId}/default")
    public ResponseEntity<Void> setDefaultAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long addressId
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        log.info("⭐ Setting default shipping address: {} for customer: {}", addressId, customerId);
        shippingAddressService.setDefaultAddress(addressId, customerId);
        return ResponseEntity.ok().build();
    }

    /**
     * Xóa địa chỉ (soft delete)
     * DELETE /api/shipping-addresses/{addressId}
     */
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long addressId
    ) {
        Integer customerId = getCustomerIdFromUserDetails(userDetails);
        log.info("🗑️ Deleting shipping address: {} for customer: {}", addressId, customerId);
        shippingAddressService.deleteAddress(addressId, customerId);
        return ResponseEntity.ok().build();
    }

    // ===== HELPER =====
    private Integer getCustomerIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Vui lòng đăng nhập");
        }
        String email = userDetails.getUsername();
        return customerRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với email: " + email))
                .getCustomerId();
    }
}