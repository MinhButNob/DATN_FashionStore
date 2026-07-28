// ChatAssignmentService.java
// VỊ TRÍ: com/example/backend_fashionstore/chat_nhan_vien/service/ChatAssignmentService.java
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Chịu trách nhiệm DUY NHẤT: gán chat WAITING cho staff phù hợp.
 *
 * LƯU Ý: KHÔNG dùng cache thủ công (liveLoadMap) để đếm tải của staff. Bản có
 * cache trước đây bị lỗi vì chỉ CỘNG khi assign() mà không bao giờ TRỪ khi
 * closeChat() -> số đếm chỉ tăng mãi -> sau vài vòng test, tất cả staff bị coi
 * là "đầy tải" dù thực tế không phải -> chat mới không gán được cho ai cả ->
 * chat kẹt ở WAITING -> customer nhắn tin không staff nào nhận được WS.
 *
 * Đếm trực tiếp từ DB (countActiveByStaff) là đủ an toàn ở đây vì:
 * - tryAssignOne/tryAssignWaitingChats đã là "synchronized" -> các lệnh gọi
 *   xử lý tuần tự trong 1 luồng.
 * - Nằm trong cùng 1 transaction Spring (@Transactional REQUIRED) -> đọc lại
 *   ngay sau khi save() trong transaction đó vẫn thấy đúng thay đổi vừa ghi
 *   (read-your-writes), không cần cache tay.
 *
 * FIX QUAN TRỌNG: gửi WS "chat_assigned" NGAY TRONG assign() - tập trung 1 chỗ
 * duy nhất, thay vì để từng nơi gọi assign() tự lo gửi. Trước đây chỉ có
 * createChat() (qua tryAssignOne) là tự gửi WS thủ công ở ngoài; còn
 * tryAssignWaitingChats() (chạy khi staff connect / khi 1 chat vừa đóng) và
 * nhánh gán lại trong sendMessage() (khi khách nhắn vào chat đang WAITING) thì
 * KHÔNG gửi gì cả. Hậu quả: FE (chatStore.js) không biết để thêm chat đó vào
 * danh sách -> lúc "new_message" bắn tới, FE tìm không thấy conversation
 * tương ứng trong state -> âm thầm bỏ qua -> staff không thấy tin nhắn mới dù
 * chat đã được gán đúng người ở DB.
 */
@Service
@RequiredArgsConstructor
public class ChatAssignmentService {

    private final ChatNhanVienRepository chatRepository;
    private final EmployeeRepository employeeRepository;
    private final ChatWebSocketHandler webSocketHandler;

    /**
     * Quét toàn bộ hàng đợi WAITING, gán từng chat cho staff rảnh nhất.
     * Dùng bởi: khi staff mới connect WS + khi 1 chat vừa được đóng.
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
                .filter(e -> e != null && isActiveStaff(e))
                .filter(e -> loadOf(e) < capOf(e))
                .min(Comparator.comparingLong(this::loadOf));
    }

    // FIX: employeeStatus = "ACTIVE" trong Employee.java chỉ là default khi tạo
    // object Java MỚI - các dòng có sẵn trong DB (tạo trước khi có cột này, hoặc
    // insert thẳng SQL) có thể đang là NULL. So sánh "ACTIVE".equals(null) ->
    // false -> staff bị loại khỏi danh sách gán chat dù đang online thật.
    // Coi NULL là ACTIVE (đúng ý định ban đầu của default value), chỉ loại khi
    // status được set rõ ràng thành giá trị khác ACTIVE.
    private boolean isActiveStaff(Employee e) {
        String status = e.getEmployeeStatus();
        return status == null || "ACTIVE".equalsIgnoreCase(status);
    }

    // Đếm SỐNG trực tiếp từ DB - luôn phản ánh đúng trạng thái thực tế.
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

        // FIX: báo cho cả 2 phía biết chat vừa được gán - bất kể assign() được
        // gọi từ đường nào (tryAssignOne, tryAssignWaitingChats, hay gián tiếp
        // qua sendMessage khi khách nhắn vào chat đang WAITING).
        Integer customerUserId = (chat.getCustomer() != null && chat.getCustomer().getUser() != null)
                ? chat.getCustomer().getUser().getUserId() : null;
        Integer staffUserId = staff.getUser().getUserId();

        Map<String, Object> payload = new HashMap<>();
        payload.put("chatId", chat.getChatId());
        payload.put("status", chat.getStatus());
        payload.put("adminId", staffUserId);
        payload.put("adminName", staff.getUser().getUsername());
        payload.put("createdAt", chat.getCreatedAt());
        if (chat.getCustomer() != null) {
            payload.put("customerName", chat.getCustomer().getCustomerName());
            payload.put("phone", chat.getCustomer().getPhone());
            payload.put("address", chat.getCustomer().getAddress());
            if (chat.getCustomer().getUser() != null) {
                payload.put("customerUserId", customerUserId);
                payload.put("customerEmail", chat.getCustomer().getUser().getEmail());
            }
        }

        webSocketHandler.sendToChatParticipants(customerUserId, staffUserId, "chat_assigned", payload);
    }
}