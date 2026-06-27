package com.example.backend_fashionstore.customer.order.repository;

import com.example.backend_fashionstore.entity.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    // Lấy bản ghi mới nhất theo orderId
    @Query("SELECT r FROM ReturnRequest r WHERE r.orderId = :orderId AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Optional<ReturnRequest> findFirstByOrderIdAndDeletedAtIsNull(@Param("orderId") Long orderId);

    // Lấy tất cả bản ghi theo orderId
    @Query("SELECT r FROM ReturnRequest r WHERE r.orderId = :orderId AND r.deletedAt IS NULL")
    List<ReturnRequest> findAllByOrderIdAndDeletedAtIsNull(@Param("orderId") Long orderId);

    // Lấy theo orderId và deletedAt null (có thể trả về nhiều)
    @Query("SELECT r FROM ReturnRequest r WHERE r.orderId = :orderId AND r.deletedAt IS NULL")
    Optional<ReturnRequest> findByOrderIdAndDeletedAtIsNull(@Param("orderId") Long orderId);

    // 🆕 Lấy danh sách orderId theo status (dùng cho filter REJECTED)
    @Query("SELECT r.orderId FROM ReturnRequest r WHERE r.status = :status AND r.deletedAt IS NULL")
    List<Long> findOrderIdsByStatus(@Param("status") String status);

    // Lấy theo customerId
    Page<ReturnRequest> findByCustomerIdAndDeletedAtIsNull(Integer customerId, Pageable pageable);

    // Lấy theo status
    Page<ReturnRequest> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);

    // Lấy tất cả chưa bị xóa
    Page<ReturnRequest> findByDeletedAtIsNull(Pageable pageable);
}