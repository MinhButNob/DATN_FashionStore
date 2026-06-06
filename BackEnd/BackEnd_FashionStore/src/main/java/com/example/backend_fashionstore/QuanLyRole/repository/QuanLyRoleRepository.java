package com.example.backend_fashionstore.QuanLyRole.repository;

import com.example.backend_fashionstore.entity.Role;
import com.example.backend_fashionstore.enums.RoleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuanLyRoleRepository extends JpaRepository<Role, Integer> {


    boolean existsByRoleName(String roleName);

    Page<Role> findByStatus(RoleStatus status, Pageable pageable);

    Page<Role> findByRoleNameContainingIgnoreCaseAndStatusNot(String roleName, RoleStatus status, Pageable pageable);

    long countByStatusNot(RoleStatus status);

    List<Role> findByStatusNot(RoleStatus status);
}

