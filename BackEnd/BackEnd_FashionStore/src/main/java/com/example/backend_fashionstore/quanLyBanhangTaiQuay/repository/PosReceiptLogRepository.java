package com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository;

import com.example.backend_fashionstore.entity.PosReceiptLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PosReceiptLogRepository extends JpaRepository<PosReceiptLog, Integer> {

    List<PosReceiptLog> findByReceiptLogOrderId(Integer orderId);
}