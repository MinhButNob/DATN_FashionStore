package com.example.backend_fashionstore.auth.repository;

import com.example.backend_fashionstore.entity.UserRole;
import com.example.backend_fashionstore.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository
        extends JpaRepository<UserRole, UserRoleId> {

    boolean existsByUser_UserIdAndRole_RoleName(Integer userId, String roleName);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM UserRole ur WHERE ur.user.userId = :userId")
    void deleteByUserId(@org.springframework.data.repository.query.Param("userId") Integer userId);
}
