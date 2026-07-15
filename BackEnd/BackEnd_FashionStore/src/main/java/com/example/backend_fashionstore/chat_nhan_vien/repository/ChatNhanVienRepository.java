// ChatNhanVienRepository.java
// VỊ TRÍ: com/example/backend_fashionstore/chat_nhan_vien/repository/ChatNhanVienRepository.java (THAY THẾ file cũ)
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

    // FIX: thêm channel="HUMAN" - tránh nhặt nhầm chat của Haru (channel="AI").
    // Đây là các query nên dùng thay cho bản không lọc channel ở trên.
    List<Chat> findByCustomer_CustomerIdAndStatusAndChannel(Integer customerId, String status, String channel);

    List<Chat> findByCustomer_User_UserId(Integer userId);

    List<Chat> findByCustomer_User_UserIdAndStatus(Integer userId, String status);

    List<Chat> findByCustomer_User_UserIdAndStatusAndChannel(Integer userId, String status, String channel);

    // FIX: dùng cho getChatsByCustomer() - liệt kê TẤT CẢ hội thoại (mọi status) của
    // khách nhưng chỉ phía "chat_nhan_vien", không lẫn lịch sử chat với Haru.
    List<Chat> findByCustomer_User_UserIdAndChannel(Integer userId, String channel);

    List<Chat> findByAdmin_UserIdAndStatus(Integer adminId, String status);

    List<Chat> findByStatus(String status);

    // FIX: lọc channel='HUMAN' - dùng bản này thay cho findByStatus() ở trên khi cần
    // danh sách chat cho dashboard staff, tránh chat của Haru lọt vào.
    List<Chat> findByStatusAndChannel(String status, String channel);

    // "Hàng đợi ảo": tất cả chat đang WAITING, xếp theo thứ tự vào trước ra trước (FIFO)
    // FIX: thêm channel='HUMAN' để phòng xa - nếu sau này có logic khác vô tình tạo
    // WAITING cho chat AI thì cũng không lọt vào hàng đợi gán nhân viên.
    @Query("SELECT c FROM Chat c WHERE c.status = 'WAITING' AND c.channel = 'HUMAN' ORDER BY c.createdAt ASC")
    List<Chat> findWaitingChatsOrderByCreatedAt();

    @Query("SELECT COUNT(c) FROM Chat c WHERE c.status = 'WAITING' AND c.channel = 'HUMAN'")
    Long countWaiting();

    // Số chat đang ACTIVE (đang xử lý) của 1 staff - dùng để chọn người ít việc nhất
    // FIX: thêm channel='HUMAN' - nếu không, nếu 1 user vừa là staff vừa vô tình có
    // chat AI (hiếm nhưng có thể) sẽ tính sai tải.
    @Query("SELECT COUNT(c) FROM Chat c WHERE c.admin.userId = :staffUserId AND c.status = 'ACTIVE' AND c.channel = 'HUMAN'")
    Long countActiveByStaff(@Param("staffUserId") Integer staffUserId);
}