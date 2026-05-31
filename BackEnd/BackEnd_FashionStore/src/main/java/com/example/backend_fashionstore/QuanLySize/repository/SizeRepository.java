package com.example.backend_fashionstore.QuanLySize.repository;

import com.example.backend_fashionstore.entity.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SizeRepository extends JpaRepository<Size,Integer> {

    boolean existsBySizeCode(String sizeCode);

    Page<Size> findBySizeCodeContainingIgnoreCaseOrSizeNameContainingIgnoreCase(
            String code,
            String name,
            Pageable pageable
    );
}