package com.example.backend_fashionstore.auth.repository;

import com.example.backend_fashionstore.entity.UserRole;
import com.example.backend_fashionstore.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository
        extends JpaRepository<UserRole, UserRoleId> {

    boolean existsByUser_UserIdAndRole_RoleName(Integer userId, String roleName);
}
