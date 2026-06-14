package com.example.backend_fashionstore.quanLyBanhangTaiQuay.repository;

import com.example.backend_fashionstore.entity.CashRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;


@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, Integer> {

    List<CashRegister> findByCashRegisterStatus(String status);

    Optional<CashRegister> findByCashRegisterName(String name);
}