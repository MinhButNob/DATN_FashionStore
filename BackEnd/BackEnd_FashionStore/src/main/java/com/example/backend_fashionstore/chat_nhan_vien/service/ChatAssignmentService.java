// ChatAssignmentService.java
// VỊ TRÍ: com/example/backend_fashionstore/chat_nhan_vien/service/ChatAssignmentService.java (FILE MỚI)
package com.example.backend_fashionstore.chat_nhan_vien.service;

import com.example.backend_fashionstore.chat_nhan_vien.repository.ChatNhanVienRepository;
import com.example.backend_fashionstore.chat_nhan_vien.websocket.ChatWebSocketHandler;
import com.example.backend_fashionstore.entity.Chat;
import com.example.backend_fashionstore.entity.Employee;
import com.example.backend_fashionstore.quanlyNhanVien.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

/**
 * Chịu trách nhiệm DUY NHẤT: gán chat WAITING cho staff phù hợp.
 *
 * Nguyên tắc (đã chốt với người dùng):
 * - KHÔNG có khái niệm online/offline riêng. "Staff sẵn sàng nhận việc" = đang
 *   có kết nối WebSocket còn sống (xem ChatWebSocketHandler.getConnectedStaffUserIds()).
 * - Trong các staff đang connect, chọn người có SỐ CHAT ACTIVE ÍT NHẤT (least-busy-first).
 *   Số lượng active được đếm SỐNG (live COUNT) từ bảng chats, không dùng counter
 *   currentChatCount cache trên Employee nữa -> tránh lệch dữ liệu khi tăng/giảm sai.
 * - Có giới hạn maxChatCount/staff để tránh 1 người bị dồn quá tải.
 */
@Service
@RequiredArgsConstructor
public class ChatAssignmentService {

    private final ChatNhanVienRepository chatRepository;
    private final EmployeeRepository employeeRepository;
    private final ChatWebSocketHandler webSocketHandler;

    /**
     * Quét toàn bộ hàng đợi WAITING, gán từng chat cho staff rảnh nhất.
     * Dùng bởi: scheduler (an toàn định kỳ) + khi staff mới connect WS + khi 1 chat vừa được đóng.
     */
    @Transactional
    public synchronized void tryAssignWaitingChats() {
        Set<Integer> connectedStaffIds = webSocketHandler.getConnectedStaffUserIds();
        if (connectedStaffIds.isEmpty()) return;

        for (Chat chat : chatRepository.findWaitingChatsOrderByCreatedAt()) {
            Optional<Employee> best = pickLeastBusyStaff(connectedStaffIds);
            if (best.isEmpty()) break; // tất cả staff đang connect đều đầy slot -> dừng, còn lại vẫn WAITING
            assign(chat, best.get());
        }
    }

    /**
     * Gán ngay 1 chat cụ thể (dùng khi khách vừa tạo chat / vừa nhắn tin khi đang WAITING).
     * Trả về true nếu gán được, false nếu phải chờ (không có staff nào rảnh).
     */
    @Transactional
    public synchronized boolean tryAssignOne(Chat chat) {
        Set<Integer> connectedStaffIds = webSocketHandler.getConnectedStaffUserIds();
        if (connectedStaffIds.isEmpty()) return false;

        Optional<Employee> best = pickLeastBusyStaff(connectedStaffIds);
        if (best.isEmpty()) return false;

        assign(chat, best.get());
        return true;
    }

    private Optional<Employee> pickLeastBusyStaff(Set<Integer> connectedStaffIds) {
        return connectedStaffIds.stream()
                .map(id -> employeeRepository.findByEmployeeUserId(id).orElse(null))
                .filter(e -> e != null && "ACTIVE".equals(e.getEmployeeStatus()))
                .filter(e -> loadOf(e) < capOf(e))
                .min(Comparator.comparingLong(this::loadOf));
    }

    private long loadOf(Employee e) {
        return chatRepository.countActiveByStaff(e.getUser().getUserId());
    }

    private int capOf(Employee e) {
        return e.getMaxChatCount() != null ? e.getMaxChatCount() : 5;
    }

    private void assign(Chat chat, Employee staff) {
        chat.setAdmin(staff.getUser());
        chat.setStatus("ACTIVE");
        chat.setAssignedAt(LocalDateTime.now());
        chatRepository.save(chat);
        System.out.println("✅ [ASSIGN] chat " + chat.getChatId() + " -> staff " + staff.getEmployeeFullname());
    }
}