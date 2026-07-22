package com.example.backend_fashionstore.QuanLyDiaChi.service;

import com.example.backend_fashionstore.QuanLyDiaChi.dto.request.ProvinceRequestDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.dto.request.WardRequestDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.ProvinceResponseDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.WardResponseDTO;

import java.util.List;

public interface AdminDiaChiService {

    // ===== QUẢN LÝ TỈNH/THÀNH PHỐ =====
    List<ProvinceResponseDTO> getAllProvinces();
    ProvinceResponseDTO getProvinceById(Integer provinceId);
    ProvinceResponseDTO createProvince(ProvinceRequestDTO request);
    ProvinceResponseDTO updateProvince(Integer provinceId, ProvinceRequestDTO request);
    void deleteProvince(Integer provinceId);
    List<ProvinceResponseDTO> searchProvinces(String keyword);

    // ===== QUẢN LÝ PHƯỜNG/XÃ =====
    List<WardResponseDTO> getAllWards();
    List<WardResponseDTO> getWardsByProvince(Integer provinceId);
    WardResponseDTO getWardById(Integer wardId);
    WardResponseDTO createWard(WardRequestDTO request);
    WardResponseDTO updateWard(Integer wardId, WardRequestDTO request);
    void deleteWard(Integer wardId);
    List<WardResponseDTO> searchWards(String keyword);
}