package com.example.backend_fashionstore.QuanLyDiaChi.service;

import com.example.backend_fashionstore.QuanLyDiaChi.dto.request.ShippingAddressRequestDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.ShippingAddressResponseDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.repository.ProvinceRepository;
import com.example.backend_fashionstore.QuanLyDiaChi.repository.WardRepository;
import com.example.backend_fashionstore.auth.repository.CustomerRepository;
import com.example.backend_fashionstore.customer.order.repository.ShippingAddressRepository;
import com.example.backend_fashionstore.entity.Customer;
import com.example.backend_fashionstore.entity.Province;
import com.example.backend_fashionstore.entity.ShippingAddress;
import com.example.backend_fashionstore.entity.Ward;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShippingAddressServiceImpl implements ShippingAddressService {

    private final ShippingAddressRepository shippingAddressRepository;
    private final CustomerRepository customerRepository;
    private final ProvinceRepository provinceRepository;
    private final WardRepository wardRepository;

    @Override
    public List<ShippingAddressResponseDTO> getAddressesByCustomer(Integer customerId) {
        log.info("📋 Getting addresses for customer: {}", customerId);
        List<ShippingAddress> addresses = shippingAddressRepository
                .findByCustomer_CustomerId(customerId);
        return addresses.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ShippingAddressResponseDTO getAddressById(Long addressId, Integer customerId) {
        log.info("📋 Getting address: {} for customer: {}", addressId, customerId);
        ShippingAddress address = shippingAddressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        if (!address.getCustomer().getCustomerId().equals(customerId)) {
            throw new RuntimeException("Địa chỉ không thuộc về khách hàng này");
        }

        return convertToResponse(address);
    }

    @Override
    public ShippingAddressResponseDTO createAddress(Integer customerId, ShippingAddressRequestDTO request) {
        log.info("➕ Creating address for customer: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        // ✅ KHÔNG LOOKUP TỪ DB LOKAL NỮA
        // Dùng dữ liệu trực tiếp từ Open API gửi lên (provinceName, districtName, wardName)
        Province province = null;
        Ward ward = null;

        // ✅ XỬ LÝ DEFAULT
        boolean isDefault = request.getIsDefault() != null && request.getIsDefault();
        if (isDefault) {
            shippingAddressRepository.findByCustomer_CustomerIdAndIsDefaultTrue(customerId)
                    .ifPresent(addr -> {
                        addr.setIsDefault(false);
                        shippingAddressRepository.save(addr);
                    });
        } else {
            long count = shippingAddressRepository.findByCustomer_CustomerId(customerId).size();
            if (count == 0) {
                isDefault = true;
            }
        }

        // ✅ TẠO SHIPPING ADDRESS
        ShippingAddress address = ShippingAddress.builder()
                .customer(customer)
                .shippingAddressFullName(request.getFullName())
                .shippingAddressPhone(request.getPhone())
                .province(province)
                .ward(ward)
                .streetAddress(request.getStreetAddress())
                .isDefault(isDefault)
                .status("ACTIVE")
                .build();

        // ✅ SET TÊN CHO CÁC FIELD CŨ (để tương thích)
        address.setShippingAddressCity(request.getProvinceName() != null ? request.getProvinceName() : "");
        address.setShippingAddressDistrict(request.getDistrictName() != null ? request.getDistrictName() : "");
        address.setShippingAddressWard(request.getWardName() != null ? request.getWardName() : "");

        // ✅ BUILD FULL ADDRESS
        address.buildFullAddress();

        ShippingAddress saved = shippingAddressRepository.save(address);
        log.info("✅ Address created: {}", saved.getShippingAddressId());
        log.info("   Full address: {}", saved.getFullAddress());

        return convertToResponse(saved);
    }

    @Override
    public ShippingAddressResponseDTO updateAddress(Long addressId, Integer customerId, ShippingAddressRequestDTO request) {
        log.info("✏️ Updating address: {} for customer: {}", addressId, customerId);

        ShippingAddress address = shippingAddressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        if (!address.getCustomer().getCustomerId().equals(customerId)) {
            throw new RuntimeException("Địa chỉ không thuộc về khách hàng này");
        }

        // ✅ CẬP NHẬT THÔNG TIN
        if (request.getFullName() != null) {
            address.setShippingAddressFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            address.setShippingAddressPhone(request.getPhone());
        }
        
        // ✅ LẤY PROVINCE VÀ WARD (Không query DB)
        address.setProvince(null);
        address.setWard(null);
        address.setShippingAddressCity(request.getProvinceName() != null ? request.getProvinceName() : "");
        address.setShippingAddressDistrict(request.getDistrictName() != null ? request.getDistrictName() : "");
        address.setShippingAddressWard(request.getWardName() != null ? request.getWardName() : "");

        if (request.getStreetAddress() != null) {
            address.setStreetAddress(request.getStreetAddress());
        }

        // ✅ CẬP NHẬT DEFAULT
        if (request.getIsDefault() != null && request.getIsDefault()) {
            shippingAddressRepository.findByCustomer_CustomerIdAndIsDefaultTrue(customerId)
                    .ifPresent(addr -> {
                        if (!addr.getShippingAddressId().equals(addressId)) {
                            addr.setIsDefault(false);
                            shippingAddressRepository.save(addr);
                        }
                    });
            address.setIsDefault(true);
        }

        address.setUpdatedAt(LocalDateTime.now());
        address.buildFullAddress();

        ShippingAddress updated = shippingAddressRepository.save(address);
        log.info("✅ Address updated: {}", updated.getShippingAddressId());

        return convertToResponse(updated);
    }

    @Override
    public void setDefaultAddress(Long addressId, Integer customerId) {
        log.info("⭐ Setting default address: {} for customer: {}", addressId, customerId);

        ShippingAddress address = shippingAddressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        if (!address.getCustomer().getCustomerId().equals(customerId)) {
            throw new RuntimeException("Địa chỉ không thuộc về khách hàng này");
        }

        shippingAddressRepository.findByCustomer_CustomerIdAndIsDefaultTrue(customerId)
                .ifPresent(addr -> {
                    addr.setIsDefault(false);
                    shippingAddressRepository.save(addr);
                });

        address.setIsDefault(true);
        address.setUpdatedAt(LocalDateTime.now());
        shippingAddressRepository.save(address);

        log.info("✅ Default address set: {}", addressId);
    }

    @Override
    public void deleteAddress(Long addressId, Integer customerId) {
        log.info("🗑️ Deleting address: {} for customer: {}", addressId, customerId);

        ShippingAddress address = shippingAddressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        if (!address.getCustomer().getCustomerId().equals(customerId)) {
            throw new RuntimeException("Địa chỉ không thuộc về khách hàng này");
        }

        if (address.getIsDefault()) {
            List<ShippingAddress> otherAddresses = shippingAddressRepository
                    .findByCustomer_CustomerId(customerId)
                    .stream()
                    .filter(a -> !a.getShippingAddressId().equals(addressId))
                    .collect(Collectors.toList());

            if (!otherAddresses.isEmpty()) {
                otherAddresses.get(0).setIsDefault(true);
                shippingAddressRepository.save(otherAddresses.get(0));
            }
        }

        address.setDeletedAt(LocalDateTime.now());
        address.setStatus("DELETED");
        shippingAddressRepository.save(address);

        log.info("✅ Address deleted: {}", addressId);
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingAddressResponseDTO getDefaultAddress(Integer customerId) {
        log.info("📋 Getting default address for customer: {}", customerId);

        ShippingAddress address = shippingAddressRepository
                .findByCustomer_CustomerIdAndIsDefaultTrueAndStatus(customerId, "ACTIVE")
                .orElseGet(() -> {
                    List<ShippingAddress> addresses = shippingAddressRepository
                            .findByCustomer_CustomerIdAndStatus(customerId, "ACTIVE");
                    if (!addresses.isEmpty()) {
                        log.info("📋 No default address, using first address: {}", addresses.get(0).getShippingAddressId());
                        return addresses.get(0);
                    }
                    return null;
                });

        if (address == null) {
            throw new RuntimeException("Không tìm thấy địa chỉ giao hàng cho khách hàng này");
        }

        return convertToResponse(address);
    }

    // ===== CONVERT METHOD =====
    private ShippingAddressResponseDTO convertToResponse(ShippingAddress address) {
        String provinceName = address.getProvince() != null ?
                address.getProvince().getProvinceName() :
                address.getShippingAddressCity();

        String wardName = address.getWard() != null ?
                address.getWard().getWardName() :
                address.getShippingAddressWard();

        String districtName = address.getWard() != null ?
                address.getWard().getDistrictName() :
                address.getShippingAddressDistrict();

        return ShippingAddressResponseDTO.builder()
                .addressId(address.getShippingAddressId())
                .provinceId(address.getProvince() != null ? address.getProvince().getProvinceId() : null)
                .provinceName(provinceName)
                .wardId(address.getWard() != null ? address.getWard().getWardId() : null)
                .wardName(wardName)
                .districtName(districtName)  // ✅ THÊM districtName
                .streetAddress(address.getStreetAddress())
                .fullAddress(address.getFullAddress())
                .fullName(address.getShippingAddressFullName())
                .phone(address.getShippingAddressPhone())
                .isDefault(address.getIsDefault())
                .status(address.getStatus())
                .build();
    }
}