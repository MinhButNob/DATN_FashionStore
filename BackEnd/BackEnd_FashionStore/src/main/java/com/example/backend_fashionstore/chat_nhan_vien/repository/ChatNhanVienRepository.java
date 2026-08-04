// ChatNhanVienRepository.java
// VỊ TRÍ: com/example/backend_fashionstore/chat_nhan_vien/repository/ChatNhanVienRepository.java
package com.example.backend_fashionstore.chat_nhan_vien.repository;

import com.example.backend_fashionstore.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatNhanVienRepository extends JpaRepository<Chat, Long> {

    List<Chat> findByCustomer_CustomerId(Integer customerId);

    List<Chat> findByCustomer_CustomerIdAndStatus(Integer customerId, String status);

    // FIX: thêm channel="HUMAN" - tránh nhặt nhầm chat của Haru (channel="AI")
    List<Chat> findByCustomer_CustomerIdAndStatusAndChannel(Integer customerId, String status, String channel);

    List<Chat> findByCustomer_User_UserId(Integer userId);

    List<Chat> findByCustomer_User_UserIdAndStatus(Integer userId, String status);

    List<Chat> findByCustomer_User_UserIdAndStatusAndChannel(Integer userId, String status, String channel);

    // FIX: dùng cho getChatsByCustomer() - liệt kê TẤT CẢ hội thoại (mọi status) của khách
    List<Chat> findByCustomer_User_UserIdAndChannel(Integer userId, String channel);

    // ===== THÊM MỚI: Lấy tất cả chat của 1 staff (mọi trạng thái) =====
    List<Chat> findByAdmin_UserId(Integer adminId);

    List<Chat> findByAdmin_UserIdAndStatus(Integer adminId, String status);

    // ===== THÊM MỚI: Lấy tất cả chat theo channel =====
    List<Chat> findByChannel(String channel);

    // ===== THÊM MỚI: Lấy tất cả chat theo status và channel =====
    List<Chat> findByStatusAndChannel(String status, String channel);

    // "Hàng đợi ảo": tất cả chat đang WAITING, xếp theo thứ tự vào trước ra trước (FIFO)
    @Query("SELECT c FROM Chat c WHERE c.status = 'WAITING' AND c.channel = 'HUMAN' ORDER BY c.createdAt ASC")
    List<Chat> findWaitingChatsOrderByCreatedAt();

    @Query("SELECT COUNT(c) FROM Chat c WHERE c.status = 'WAITING' AND c.channel = 'HUMAN'")
    Long countWaiting();

    // Số chat đang ACTIVE (đang xử lý) của 1 staff
    @Query("SELECT COUNT(c) FROM Chat c WHERE c.admin.userId = :staffUserId AND c.status = 'ACTIVE' AND c.channel = 'HUMAN'")
    Long countActiveByStaff(@Param("staffUserId") Integer staffUserId);
}