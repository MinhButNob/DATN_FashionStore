package com.example.backend_fashionstore.QuanLyDiaChi.service;

import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.ProvinceResponseDTO;
import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.WardResponseDTO;

import java.util.List;

public interface CustomerDiaChiService {
    List<ProvinceResponseDTO> getActiveProvinces();
    List<WardResponseDTO> getActiveWardsByProvince(Integer provinceId);
}