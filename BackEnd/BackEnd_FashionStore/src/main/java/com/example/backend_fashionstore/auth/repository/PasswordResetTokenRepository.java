// PasswordResetTokenRepository.java
package com.example.backend_fashionstore.auth.repository;

import com.example.backend_fashionstore.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByEmailAndOtpAndIsUsedFalse(String email, String otp);

    @Modifying
    @Transactional
    @Query("UPDATE PasswordResetToken t SET t.isUsed = true WHERE t.email = :email AND t.otp = :otp")
    void markAsUsed(@Param("email") String email, @Param("otp") String otp);

    void deleteByEmail(String email);
}