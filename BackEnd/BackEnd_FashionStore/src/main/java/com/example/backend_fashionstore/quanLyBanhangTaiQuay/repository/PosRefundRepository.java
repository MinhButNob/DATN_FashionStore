package com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository;

import com.example.backend_fashionstore.entity.PosRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PosRefundRepository extends JpaRepository<PosRefund, Integer> {

    List<PosRefund> findByPosRefundOrderId(Integer orderId);

    List<PosRefund> findByPosRefundStatus(String status);
}
