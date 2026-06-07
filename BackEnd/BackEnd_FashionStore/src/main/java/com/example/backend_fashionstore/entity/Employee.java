package com.example.backend_fashionstore.entity;

import com.example.backend_fashionstore.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Integer employeeId;

    // ✅ SỬA: Liên kết với bảng users
    @OneToOne
    @JoinColumn(name = "employee_user_id", nullable = false)
    private User user;

    @Column(name = "employee_fullname", nullable = false)
    private String employeeFullname;

    @Column(name = "employee_gender", nullable = false)
    private Boolean employeeGender;

//    @Column(name = "employee_age")
//    private Integer employeeAge;

    @Column(name = "employee_phone", nullable = false)
    private String employeePhone;

    @Column(name = "employee_email")
    private String employeeEmail;

    @Column(name = "employee_status")
    private String employeeStatus = "ACTIVE";

    @Column(name = "employee_position")
    private String employeePosition;

    @Column(name = "employee_created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "employee_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "employee_deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (employeeStatus == null) {
            employeeStatus = "ACTIVE";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method để lấy status từ User
    public UserStatus getUserStatus() {
        return user != null ? user.getStatus() : null;
    }

    // Helper method để kiểm tra tài khoản có bị khóa không
    public boolean isLocked() {
        return user != null && user.getStatus() == UserStatus.BANNED;
    }

    // Helper method để kiểm tra tài khoản có bị xóa không
    public boolean isDeleted() {
        return user != null && user.getStatus() == UserStatus.DELETED;
    }
}