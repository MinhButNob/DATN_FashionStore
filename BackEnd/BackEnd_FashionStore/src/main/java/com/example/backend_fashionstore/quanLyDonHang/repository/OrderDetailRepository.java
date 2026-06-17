package com.example.backend_fashionstore.QuanLyDonHang.repository;

import com.example.backend_fashionstore.entity.OrderDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

    List<OrderDetail> findByOrder_OrderId(Long orderId);

    // ✅ SỬA: Dùng native query cho top selling products
    @Query(value = "SELECT od.product_detail_id, SUM(od.quantity) as total_quantity " +
            "FROM order_details od " +
            "JOIN orders o ON od.order_detail_order_id = o.order_id " +
            "WHERE o.order_deleted_at IS NULL AND o.order_status = 'COMPLETED' " +
            "GROUP BY od.product_detail_id " +
            "ORDER BY total_quantity DESC", nativeQuery = true)
    List<Object[]> getTopSellingProducts(Pageable pageable);
}