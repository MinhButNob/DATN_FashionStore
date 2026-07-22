package com.example.backend_fashionstore.QuanLyDiaChi.service;

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

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerDiaChiServiceImpl implements CustomerDiaChiService {

    private final ProvinceRepository provinceRepository;
    private final WardRepository wardRepository;

    @Override
    public List<ProvinceResponseDTO> getActiveProvinces() {
        log.info("📋 Getting all active provinces for customer");
        List<Province> provinces = provinceRepository.findAllActive();
        return provinces.stream()
                .map(this::convertToProvinceResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WardResponseDTO> getActiveWardsByProvince(Integer provinceId) {
        log.info("📋 Getting active wards for province: {}", provinceId);
        List<Ward> wards = wardRepository.findByProvinceIdAndStatusActive(provinceId);
        return wards.stream()
                .map(this::convertToWardResponse)
                .collect(Collectors.toList());
    }

    private ProvinceResponseDTO convertToProvinceResponse(Province province) {
        return ProvinceResponseDTO.builder()
                .provinceId(province.getProvinceId())
                .provinceName(province.getProvinceName())
                .provinceCode(province.getProvinceCode())
                .provinceType(province.getProvinceType())
                .displayOrder(province.getDisplayOrder())
                .status(province.getStatus())
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