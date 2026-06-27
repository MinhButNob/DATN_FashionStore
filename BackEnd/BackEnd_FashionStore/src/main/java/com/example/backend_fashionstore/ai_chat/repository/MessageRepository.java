package com.example.backend_fashionstore.ai_chat.repository;

import com.example.backend_fashionstore.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChat_ChatIdOrderBySentDateAsc(Long chatId);
}
