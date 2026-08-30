package com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository;

import com.example.backend_fashionstore.entity.PosReturnExchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PosReturnExchangeRepository extends JpaRepository<PosReturnExchange, Integer> {

    List<PosReturnExchange> findByOrderId(Integer orderId);

    @Query("SELECT re FROM PosReturnExchange re WHERE re.customerPhone LIKE %:phone%")
    List<PosReturnExchange> findByCustomerPhoneContaining(@Param("phone") String phone);

    List<PosReturnExchange> findByStatus(String status);
}