package com.example.backend_fashionstore.QuanLyRole.service;

import com.example.backend_fashionstore.QuanLyRole.dto.RoleRequestDTO;
import com.example.backend_fashionstore.QuanLyRole.dto.RoleResponseDTO;
import com.example.backend_fashionstore.QuanLyRole.repository.QuanLyRoleRepository;
import com.example.backend_fashionstore.entity.Role;
import com.example.backend_fashionstore.enums.RoleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final QuanLyRoleRepository roleRepository;

    private RoleResponseDTO convert(Role role) {
        String statusString = null;
        if (role.getStatus() != null) {
            statusString = role.getStatus().name();
        }

        return RoleResponseDTO.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .status(statusString)
                .build();
    }

    @Override
    public Page<RoleResponseDTO> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Role> rolePage = roleRepository.findAll(pageable);
        return rolePage.map(this::convert);
    }

    @Override
    public Page<RoleResponseDTO> filterByStatus(Boolean status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Role> rolePage;

        if (status == null) {
            rolePage = roleRepository.findAll(pageable);
        } else if (status) {
            rolePage = roleRepository.findByStatus(RoleStatus.ACTIVE, pageable);
        } else {
            rolePage = roleRepository.findByStatus(RoleStatus.INACTIVE, pageable);
        }

        return rolePage.map(this::convert);
    }

    @Override
    public RoleResponseDTO getById(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
        return convert(role);
    }

    @Override
    public RoleResponseDTO create(RoleRequestDTO request) {
        if (roleRepository.existsByRoleName(request.getRoleName())) {
            throw new RuntimeException("Tên role đã tồn tại");
        }

        RoleStatus roleStatus = RoleStatus.ACTIVE;
        if (request.getStatus() != null) {
            try {
                roleStatus = RoleStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                roleStatus = RoleStatus.ACTIVE;
            }
        }

        Role role = Role.builder()
                .roleName(request.getRoleName())
                .status(roleStatus)
                .build();

        Role savedRole = roleRepository.save(role);
        return convert(savedRole);
    }

    @Override
    public RoleResponseDTO update(Integer id, RoleRequestDTO request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));

        role.setRoleName(request.getRoleName());

        if (request.getStatus() != null) {
            try {
                RoleStatus newStatus = RoleStatus.valueOf(request.getStatus().toUpperCase());
                role.setStatus(newStatus);
            } catch (IllegalArgumentException e) {
                // Giữ nguyên status cũ
            }
        }

        Role updatedRole = roleRepository.save(role);
        return convert(updatedRole);
    }

    @Override
    public void delete(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
        role.setStatus(RoleStatus.DELETED);
        role.setDeletedAt(LocalDateTime.now());
        roleRepository.save(role);
    }

    @Override
    public Page<RoleResponseDTO> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Role> rolePage = roleRepository.findByRoleNameContainingIgnoreCaseAndStatusNot(
                keyword, RoleStatus.DELETED, pageable);
        return rolePage.map(this::convert);
    }

    @Override
    public long count() {
        return roleRepository.countByStatusNot(RoleStatus.DELETED);
    }

    @Override
    public List<RoleResponseDTO> getAllRoles() {
        List<Role> roles = roleRepository.findByStatusNot(RoleStatus.DELETED);
        return roles.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }
}