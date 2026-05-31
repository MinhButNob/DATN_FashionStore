package com.example.backend_fashionstore.QuanLySize.service;

import com.example.backend_fashionstore.QuanLySize.dto.SizeRequestDTO;
import com.example.backend_fashionstore.QuanLySize.dto.SizeResponseDTO;
import org.springframework.data.domain.Page;

public interface SizeService {

    Page<SizeResponseDTO> getAll(int page, int size);

    SizeResponseDTO getById(Integer id);

    SizeResponseDTO create(SizeRequestDTO request);

    SizeResponseDTO update(Integer id, SizeRequestDTO request);

    void delete(Integer id);

    Page<SizeResponseDTO> search(String keyword, int page, int size);

    long count();
}