package com.example.backend_fashionstore.QuanLyRole.service;

import com.example.backend_fashionstore.QuanLyRole.dto.RoleRequestDTO;
import com.example.backend_fashionstore.QuanLyRole.dto.RoleResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface RoleService {
    Page<RoleResponseDTO> getAll(int page, int size);
    Page<RoleResponseDTO> filterByStatus(Boolean status, int page, int size);
    RoleResponseDTO getById(Integer id);
    RoleResponseDTO create(RoleRequestDTO request);
    RoleResponseDTO update(Integer id, RoleRequestDTO request);
    void delete(Integer id);
    Page<RoleResponseDTO> search(String keyword, int page, int size);
    long count();
    List<RoleResponseDTO> getAllRoles();
}

