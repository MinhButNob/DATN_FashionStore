package com.example.backend_fashionstore.QuanLyDiaChi.service;

import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.AddressLookupResponse;
import java.util.List;

public interface AddressLookupService {
    List<AddressLookupResponse> getProvinces();
    List<AddressLookupResponse> getDistricts(String provinceCode);
    List<AddressLookupResponse> getWards(String districtCode);
}
