package com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository;

import com.example.backend_fashionstore.entity.PosOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PosOrderItemRepository extends JpaRepository<PosOrderItem, Integer> {

    List<PosOrderItem> findByPosOrderItemOrderId(Integer orderId);

    @Query("SELECT i FROM PosOrderItem i WHERE i.posOrderItemOrderId IN " +
            "(SELECT o.posOrderId FROM PosOrder o WHERE o.posOrderSessionId = :sessionId)")
    List<PosOrderItem> findBySessionId(@Param("sessionId") Integer sessionId);
}