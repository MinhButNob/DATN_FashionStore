package com.example.backend_fashionstore.chat_nhan_vien.service;

import com.example.backend_fashionstore.entity.Employee;
import com.example.backend_fashionstore.quanlyNhanVien.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffStatusService {

    private final EmployeeRepository employeeRepository;

    // Tăng số chat đang xử lý
    public void incrementChatCount(Integer employeeId) {
        Employee staff = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        int newCount = currentChatCount(staff) + 1;
        staff.setCurrentChatCount(newCount);
        staff.setLastActiveAt(LocalDateTime.now());
        employeeRepository.save(staff);

        System.out.println("✅ [STAFF] incrementChatCount: employeeId=" + employeeId +
                ", newCount=" + newCount);
    }

    // Giảm số chat đang xử lý
    public void decrementChatCount(Integer employeeId) {
        Employee staff = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        int newCount = Math.max(0, currentChatCount(staff) - 1);
        staff.setCurrentChatCount(newCount);
        staff.setLastActiveAt(LocalDateTime.now());
        employeeRepository.save(staff);

        System.out.println("✅ [STAFF] decrementChatCount: employeeId=" + employeeId +
                ", newCount=" + newCount);
    }

    // BỎ setOnline, setOffline - không cần nữa
    // Khách hàng nhắn là tự động tìm staff có ít chat nhất

//    // Lấy số chat hiện tại của staff
//    public Integer getCurrentChatCount(Integer userId) {
//        Employee staff = employeeRepository.findByUser_UserId(userId)
//                .orElseThrow(() -> new RuntimeException("Staff not found"));
//        return currentChatCount(staff);
//    }

    private Integer currentChatCount(Employee staff) {
        return staff.getCurrentChatCount() != null ? staff.getCurrentChatCount() : 0;
    }

    private Integer maxChatCount(Employee staff) {
        return staff.getMaxChatCount() != null ? staff.getMaxChatCount() : 5;
    }
}