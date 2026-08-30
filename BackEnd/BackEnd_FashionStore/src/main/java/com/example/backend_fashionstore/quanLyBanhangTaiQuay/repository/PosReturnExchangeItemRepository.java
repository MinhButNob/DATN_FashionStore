package com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository;

import com.example.backend_fashionstore.entity.PosReturnExchangeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PosReturnExchangeItemRepository extends JpaRepository<PosReturnExchangeItem, Integer> {

    List<PosReturnExchangeItem> findByReturnExchangeId(Integer returnExchangeId);

    List<PosReturnExchangeItem> findByReturnExchangeIdAndType(Integer returnExchangeId, String type);
}