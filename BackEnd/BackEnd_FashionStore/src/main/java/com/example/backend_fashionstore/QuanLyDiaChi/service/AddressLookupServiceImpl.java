package com.example.backend_fashionstore.QuanLyDiaChi.service;

import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.AddressLookupResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressLookupServiceImpl implements AddressLookupService {
    private static final String API_URL = "https://provinces.open-api.vn/api";
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<AddressLookupResponse> getProvinces() {
        return get("/p/");
    }

    @Override
    public List<AddressLookupResponse> getDistricts(String provinceCode) {
        requireCode(provinceCode, "provinceCode");
        return getNested("/p/" + provinceCode + "?depth=2", "districts");
    }

    @Override
    public List<AddressLookupResponse> getWards(String districtCode) {
        requireCode(districtCode, "districtCode");
        return getNested("/d/" + districtCode + "?depth=2", "wards");
    }

    private List<AddressLookupResponse> get(String path) {
        try {
            String body = restTemplate.getForObject(API_URL + path, String.class);
            JsonNode root = objectMapper.readTree(body);
            List<AddressLookupResponse> result = new ArrayList<>();
            root.forEach(node -> result.add(AddressLookupResponse.builder()
                    .code(node.path("code").asText())
                    .name(node.path("name").asText())
                    .build()));
            return result;
        } catch (Exception ex) {
            log.error("Address Open API lookup failed: path={}", path, ex);
            throw new IllegalStateException("Không thể tải dữ liệu địa chỉ chuẩn", ex);
        }
    }

    private List<AddressLookupResponse> getNested(String path, String field) {
        try {
            String body = restTemplate.getForObject(API_URL + path, String.class);
            JsonNode root = objectMapper.readTree(body).path(field);
            List<AddressLookupResponse> result = new ArrayList<>();
            root.forEach(node -> result.add(AddressLookupResponse.builder()
                    .code(node.path("code").asText())
                    .name(node.path("name").asText())
                    .build()));
            return result;
        } catch (Exception ex) {
            log.error("Address Open API lookup failed: path={}, field={}", path, field, ex);
            throw new IllegalStateException("Không thể tải dữ liệu địa chỉ chuẩn", ex);
        }
    }

    private void requireCode(String code, String field) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(field + " không được để trống");
        }
    }
}
