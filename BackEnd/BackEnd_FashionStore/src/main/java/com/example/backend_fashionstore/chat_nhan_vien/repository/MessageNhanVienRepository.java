package com.example.backend_fashionstore.chat_nhan_vien.repository;

import com.example.backend_fashionstore.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageNhanVienRepository extends JpaRepository<Message, Long> {

    List<Message> findByChat_ChatIdOrderBySentDateAsc(Long chatId);

    @Query("SELECT m FROM Message m WHERE m.chat.chatId = :chatId AND m.isRead = false AND m.user.userId != :userId")
    List<Message> findUnreadMessages(@Param("chatId") Long chatId, @Param("userId") Integer userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chat.chatId = :chatId AND m.isRead = false AND m.user.userId != :userId")
    Long countUnreadMessages(@Param("chatId") Long chatId, @Param("userId") Integer userId);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.chat.chatId = :chatId AND m.isRead = false AND m.user.userId != :userId")
    int markAllAsRead(@Param("chatId") Long chatId, @Param("userId") Integer userId);

    @Query("SELECT m FROM Message m WHERE m.chat.chatId = :chatId ORDER BY m.sentDate DESC")
    List<Message> findLastMessage(@Param("chatId") Long chatId, org.springframework.data.domain.Pageable pageable);
}