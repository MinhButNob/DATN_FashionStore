package com.example.backend_fashionstore.QuanLyMauSac.service;

import com.example.backend_fashionstore.QuanLyMauSac.dto.request.ColorRequestDTO;
import com.example.backend_fashionstore.QuanLyMauSac.dto.response.ColorResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ColorService {

    Page<ColorResponseDTO> getAll(int page, int size);

    List<ColorResponseDTO> getAllActive();

    ColorResponseDTO getById(Integer id);

    ColorResponseDTO create(ColorRequestDTO request);

    ColorResponseDTO update(Integer id, ColorRequestDTO request);

    void delete(Integer id);

    void restore(Integer id);

    Page<ColorResponseDTO> search(String keyword, int page, int size);

    long count();
}