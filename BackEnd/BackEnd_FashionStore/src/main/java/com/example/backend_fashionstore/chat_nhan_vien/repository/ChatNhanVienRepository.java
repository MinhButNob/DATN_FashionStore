package com.example.backend_fashionstore.chat_nhan_vien.repository;

import com.example.backend_fashionstore.entity.Chat;
import com.example.backend_fashionstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatNhanVienRepository extends JpaRepository<Chat, Long> {

    List<Chat> findByCustomer_CustomerId(Integer customerId);

    List<Chat> findByCustomer_CustomerIdAndStatus(Integer customerId, String status);

    Optional<Chat> findOptionalByCustomer_CustomerIdAndStatus(Integer customerId, String status);

    List<Chat> findByAdmin_UserIdAndStatus(Integer adminId, String status);

    List<Chat> findByStatus(String status);

    @Query("SELECT COUNT(c) FROM Chat c WHERE c.admin IS NULL AND c.status = 'ACTIVE'")
    Long countUnassignedChats();

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.admin = :admin WHERE c.chatId = :chatId")
    int assignAdminToChat(@Param("chatId") Long chatId, @Param("admin") User admin);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.status = :status WHERE c.chatId = :chatId")
    int updateChatStatus(@Param("chatId") Long chatId, @Param("status") String status);
}