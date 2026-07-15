package com.example.backend_fashionstore.ai_chat.repository;

import com.example.backend_fashionstore.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    Optional<Chat> findByCustomer_CustomerIdAndStatusAndChannel(Integer customerId, String status, String channel);
//    Optional<Chat> findByCustomer_CustomerIdAndStatus(Integer customerId, String status);
}
