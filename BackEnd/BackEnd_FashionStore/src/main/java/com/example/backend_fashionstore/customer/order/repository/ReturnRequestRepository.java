package com.example.backend_fashionstore.customer.order.repository;

import com.example.backend_fashionstore.entity.ReturnRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReturnRequest r WHERE r.returnRequestId = :id AND r.deletedAt IS NULL")
    Optional<ReturnRequest> findForInspection(@Param("id") Long id);

    // Lấy bản ghi mới nhất theo orderId
    @Query("SELECT r FROM ReturnRequest r WHERE r.orderId = :orderId AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Optional<ReturnRequest> findFirstByOrderIdAndDeletedAtIsNull(@Param("orderId") Long orderId);

    // Lấy tất cả bản ghi theo orderId
    @Query("SELECT r FROM ReturnRequest r WHERE r.orderId = :orderId AND r.deletedAt IS NULL")
    List<ReturnRequest> findAllByOrderIdAndDeletedAtIsNull(@Param("orderId") Long orderId);

    // Lấy theo orderId và deletedAt null
    @Query("SELECT r FROM ReturnRequest r WHERE r.orderId = :orderId AND r.deletedAt IS NULL")
    Optional<ReturnRequest> findByOrderIdAndDeletedAtIsNull(@Param("orderId") Long orderId);

    // Lấy danh sách orderId theo status (dùng cho filter REJECTED)
    @Query("SELECT r.orderId FROM ReturnRequest r WHERE r.status = :status AND r.deletedAt IS NULL")
    List<Long> findOrderIdsByStatus(@Param("status") String status);

    // Lấy theo customerId
    Page<ReturnRequest> findByCustomerIdAndDeletedAtIsNull(Integer customerId, Pageable pageable);

    // Lấy theo status
    Page<ReturnRequest> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);

    // Lấy tất cả chưa bị xóa
    Page<ReturnRequest> findByDeletedAtIsNull(Pageable pageable);

    // 🆕 Kiểm tra tồn tại yêu cầu hoàn hàng theo orderId và status
    boolean existsByOrderIdAndStatusAndDeletedAtIsNull(Long orderId, String status);

    // 🆕 Lấy danh sách yêu cầu hoàn hàng theo orderId và status
    @Query("SELECT r FROM ReturnRequest r WHERE r.orderId = :orderId AND r.status = :status AND r.deletedAt IS NULL")
    Optional<ReturnRequest> findByOrderIdAndStatusAndDeletedAtIsNull(@Param("orderId") Long orderId, @Param("status") String status);
}
