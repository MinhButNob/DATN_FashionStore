package com.example.backend_fashionstore.QuanLyDiaChi.service;

import com.example.backend_fashionstore.QuanLyDiaChi.dto.request.ProvinceRequestDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.dto.request.WardRequestDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.ProvinceResponseDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.WardResponseDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.repository.ProvinceRepository;
import com.example.backend_fashionstore.QuanLyDiaChi.repository.WardRepository;
import com.example.backend_fashionstore.entity.Province;
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
public class AdminDiaChiServiceImpl implements AdminDiaChiService {

    private final ProvinceRepository provinceRepository;
    private final WardRepository wardRepository;

    // ================================================================
    // ===== PROVINCE =====
    // ================================================================

    @Override
    public List<ProvinceResponseDTO> getAllProvinces() {
        log.info("📋 Getting all provinces");
        List<Province> provinces = provinceRepository.findAllWithDeletedAtNull();
        return provinces.stream()
                .map(this::convertToProvinceResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProvinceResponseDTO getProvinceById(Integer provinceId) {
        log.info("📋 Getting province by id: {}", provinceId);
        Province province = provinceRepository.findById(provinceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tỉnh/thành phố với ID: " + provinceId));
        return convertToProvinceResponse(province);
    }

    @Override
    public ProvinceResponseDTO createProvince(ProvinceRequestDTO request) {
        log.info("➕ Creating province: {}", request.getProvinceName());

        if (provinceRepository.existsByProvinceCodeAndDeletedAtIsNull(request.getProvinceCode())) {
            throw new RuntimeException("Mã tỉnh/thành phố đã tồn tại: " + request.getProvinceCode());
        }

        Province province = Province.builder()
                .provinceName(request.getProvinceName())
                .provinceCode(request.getProvinceCode())
                .provinceType(request.getProvinceType())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .status("ACTIVE")
                .build();

        Province saved = provinceRepository.save(province);
        log.info("✅ Province created successfully: {}", saved.getProvinceId());

        return convertToProvinceResponse(saved);
    }

    @Override
    public ProvinceResponseDTO updateProvince(Integer provinceId, ProvinceRequestDTO request) {
        log.info("✏️ Updating province: {}", provinceId);

        Province province = provinceRepository.findById(provinceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tỉnh/thành phố với ID: " + provinceId));

        if (!province.getProvinceCode().equals(request.getProvinceCode()) &&
                provinceRepository.existsByProvinceCodeAndDeletedAtIsNull(request.getProvinceCode())) {
            throw new RuntimeException("Mã tỉnh/thành phố đã tồn tại: " + request.getProvinceCode());
        }

        province.setProvinceName(request.getProvinceName());
        province.setProvinceCode(request.getProvinceCode());
        province.setProvinceType(request.getProvinceType());
        province.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        province.setUpdatedAt(LocalDateTime.now());

        Province updated = provinceRepository.save(province);
        log.info("✅ Province updated successfully: {}", updated.getProvinceId());

        return convertToProvinceResponse(updated);
    }

    @Override
    public void deleteProvince(Integer provinceId) {
        log.info("🗑️ Deleting province: {}", provinceId);

        Province province = provinceRepository.findById(provinceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tỉnh/thành phố với ID: " + provinceId));

        List<Ward> activeWards = wardRepository.findByProvinceIdAndStatusActive(provinceId);
        if (!activeWards.isEmpty()) {
            throw new RuntimeException("Không thể xóa tỉnh/thành phố vì còn " + activeWards.size() + " phường/xã đang hoạt động");
        }

        province.setStatus("DELETED");
        province.setDeletedAt(LocalDateTime.now());
        provinceRepository.save(province);

        log.info("✅ Province deleted successfully: {}", provinceId);
    }

    @Override
    public List<ProvinceResponseDTO> searchProvinces(String keyword) {
        log.info("🔍 Searching provinces with keyword: {}", keyword);
        List<Province> provinces = provinceRepository.searchByName(keyword);
        return provinces.stream()
                .map(this::convertToProvinceResponse)
                .collect(Collectors.toList());
    }

    // ================================================================
    // ===== WARD =====
    // ================================================================

    // ✅ THÊM MỚI: Lấy tất cả phường/xã
    @Override
    public List<WardResponseDTO> getAllWards() {
        log.info("📋 Getting all wards");
        List<Ward> wards = wardRepository.findAllWithDeletedAtNull();
        return wards.stream()
                .map(this::convertToWardResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WardResponseDTO> getWardsByProvince(Integer provinceId) {
        log.info("📋 Getting wards for province: {}", provinceId);
        List<Ward> wards = wardRepository.findByProvinceIdAndStatusActive(provinceId);
        return wards.stream()
                .map(this::convertToWardResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WardResponseDTO getWardById(Integer wardId) {
        log.info("📋 Getting ward by id: {}", wardId);
        Ward ward = wardRepository.findById(wardId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phường/xã với ID: " + wardId));
        return convertToWardResponse(ward);
    }

    @Override
    public WardResponseDTO createWard(WardRequestDTO request) {
        log.info("➕ Creating ward: {}", request.getWardName());

        Province province = provinceRepository.findById(request.getProvinceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tỉnh/thành phố với ID: " + request.getProvinceId()));

        if (wardRepository.existsByWardCodeAndDeletedAtIsNull(request.getWardCode())) {
            throw new RuntimeException("Mã phường/xã đã tồn tại: " + request.getWardCode());
        }

        Ward ward = Ward.builder()
                .province(province)
                .wardName(request.getWardName())
                .wardCode(request.getWardCode())
                .wardType(request.getWardType())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .status("ACTIVE")
                .build();

        Ward saved = wardRepository.save(ward);
        log.info("✅ Ward created successfully: {}", saved.getWardId());

        return convertToWardResponse(saved);
    }

    @Override
    public WardResponseDTO updateWard(Integer wardId, WardRequestDTO request) {
        log.info("✏️ Updating ward: {}", wardId);

        Ward ward = wardRepository.findById(wardId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phường/xã với ID: " + wardId));

        if (!ward.getProvince().getProvinceId().equals(request.getProvinceId())) {
            Province province = provinceRepository.findById(request.getProvinceId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tỉnh/thành phố với ID: " + request.getProvinceId()));
            ward.setProvince(province);
        }

        if (!ward.getWardCode().equals(request.getWardCode()) &&
                wardRepository.existsByWardCodeAndDeletedAtIsNull(request.getWardCode())) {
            throw new RuntimeException("Mã phường/xã đã tồn tại: " + request.getWardCode());
        }

        ward.setWardName(request.getWardName());
        ward.setWardCode(request.getWardCode());
        ward.setWardType(request.getWardType());
        ward.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        ward.setUpdatedAt(LocalDateTime.now());

        Ward updated = wardRepository.save(ward);
        log.info("✅ Ward updated successfully: {}", updated.getWardId());

        return convertToWardResponse(updated);
    }

    @Override
    public void deleteWard(Integer wardId) {
        log.info("🗑️ Deleting ward: {}", wardId);

        Ward ward = wardRepository.findById(wardId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phường/xã với ID: " + wardId));

        ward.setStatus("DELETED");
        ward.setDeletedAt(LocalDateTime.now());
        wardRepository.save(ward);

        log.info("✅ Ward deleted successfully: {}", wardId);
    }

    @Override
    public List<WardResponseDTO> searchWards(String keyword) {
        log.info("🔍 Searching wards with keyword: {}", keyword);
        List<Ward> wards = wardRepository.searchByName(keyword);
        return wards.stream()
                .map(this::convertToWardResponse)
                .collect(Collectors.toList());
    }

    // ================================================================
    // ===== CONVERT METHODS =====
    // ================================================================

    private ProvinceResponseDTO convertToProvinceResponse(Province province) {
        List<WardResponseDTO> wardDTOs = province.getWards().stream()
                .filter(w -> w.getDeletedAt() == null)
                .map(this::convertToWardResponse)
                .collect(Collectors.toList());

        return ProvinceResponseDTO.builder()
                .provinceId(province.getProvinceId())
                .provinceName(province.getProvinceName())
                .provinceCode(province.getProvinceCode())
                .provinceType(province.getProvinceType())
                .displayOrder(province.getDisplayOrder())
                .status(province.getStatus())
                .wards(wardDTOs)
                .build();
    }

    private WardResponseDTO convertToWardResponse(Ward ward) {
        return WardResponseDTO.builder()
                .wardId(ward.getWardId())
                .wardName(ward.getWardName())
                .wardCode(ward.getWardCode())
                .wardType(ward.getWardType())
                .displayOrder(ward.getDisplayOrder())
                .status(ward.getStatus())
                .provinceId(ward.getProvince() != null ? ward.getProvince().getProvinceId() : null)
                .provinceName(ward.getProvince() != null ? ward.getProvince().getProvinceName() : null)
                .build();
    }
}