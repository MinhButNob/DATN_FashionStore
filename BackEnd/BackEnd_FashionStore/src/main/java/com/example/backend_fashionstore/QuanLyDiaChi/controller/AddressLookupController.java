package com.example.backend_fashionstore.QuanLyDiaChi.controller;

import com.example.backend_fashionstore.QuanLyDiaChi.dto.response.AddressLookupResponse;
import com.example.backend_fashionstore.QuanLyDiaChi.service.AddressLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address-lookup")
@RequiredArgsConstructor
public class AddressLookupController {
    private final AddressLookupService addressLookupService;

    @GetMapping("/provinces")
    public ResponseEntity<List<AddressLookupResponse>> provinces() {
        return ResponseEntity.ok(addressLookupService.getProvinces());
    }

    @GetMapping("/districts")
    public ResponseEntity<List<AddressLookupResponse>> districts(@RequestParam String provinceCode) {
        return ResponseEntity.ok(addressLookupService.getDistricts(provinceCode));
    }

    @GetMapping("/wards")
    public ResponseEntity<List<AddressLookupResponse>> wards(@RequestParam String districtCode) {
        return ResponseEntity.ok(addressLookupService.getWards(districtCode));
    }
}
