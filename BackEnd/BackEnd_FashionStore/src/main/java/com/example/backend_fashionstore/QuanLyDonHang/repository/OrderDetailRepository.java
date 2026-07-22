package com.example.backend_fashionstore.QuanLyDonHang.repository;

import com.example.backend_fashionstore.entity.OrderDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

    /**
     * Lấy danh sách order detail theo orderId
     */
    List<OrderDetail> findByOrder_OrderId(Long orderId);

    /**
     * Lấy top sản phẩm bán chạy (chỉ lấy product_detail_id)
     */
    @Query(value = "SELECT od.product_detail_id, SUM(od.quantity) as total_quantity " +
            "FROM order_details od " +
            "JOIN orders o ON od.order_detail_order_id = o.order_id " +
            "WHERE o.order_deleted_at IS NULL AND o.order_status = 'COMPLETED' " +
            "GROUP BY od.product_detail_id " +
            "ORDER BY total_quantity DESC", nativeQuery = true)
    List<Object[]> getTopSellingProducts(Pageable pageable);

    // ===== DASHBOARD - THÊM MỚI =====

    /**
     * Lấy top sản phẩm bán chạy với chi tiết đầy đủ (tên, ảnh, doanh thu)
     */
    @Query(value = """
        SELECT 
            p.product_id,
            p.product_name,
            COALESCE(SUM(od.quantity), 0) as total_sold,
            COALESCE(SUM(od.final_amount), 0) as total_revenue,
            (
                SELECT pi.product_image_path 
                FROM product_images pi 
                WHERE pi.product_image_product_detail_id = pd.product_detail_id 
                LIMIT 1
            ) as image_url
        FROM order_details od
        JOIN orders o ON od.order_detail_order_id = o.order_id
        JOIN product_details pd ON od.order_detail_product_detail_id = pd.product_detail_id
        JOIN products p ON pd.product_detail_product_id = p.product_id
        LEFT JOIN product_images pi ON pd.product_detail_id = pi.product_image_product_detail_id
        WHERE o.order_deleted_at IS NULL 
            AND o.order_status = 'COMPLETED'
            AND p.product_deleted_at IS NULL
        GROUP BY p.product_id, p.product_name, pd.product_detail_id
        ORDER BY total_sold DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> getTopSellingProductsWithDetails(@Param("limit") int limit);

    /**
     * Lấy tổng doanh thu theo sản phẩm (dùng cho thống kê)
     */
    @Query(value = """
        SELECT 
            p.product_id,
            p.product_name,
            COALESCE(SUM(od.final_amount), 0) as total_revenue
        FROM order_details od
        JOIN orders o ON od.order_detail_order_id = o.order_id
        JOIN product_details pd ON od.order_detail_product_detail_id = pd.product_detail_id
        JOIN products p ON pd.product_detail_product_id = p.product_id
        WHERE o.order_deleted_at IS NULL 
            AND o.order_status = 'COMPLETED'
            AND p.product_deleted_at IS NULL
            AND EXTRACT(YEAR FROM o.order_created_at) = :year
            AND EXTRACT(MONTH FROM o.order_created_at) = :month
        GROUP BY p.product_id, p.product_name
        ORDER BY total_revenue DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> getTopRevenueProductsByMonth(
            @Param("year") int year,
            @Param("month") int month,
            @Param("limit") int limit
    );

    /**
     * Lấy tổng số lượng sản phẩm đã bán theo product_detail_id
     */
    @Query("SELECT SUM(od.quantity) FROM OrderDetail od " +
            "WHERE od.productDetail.productDetailId = :productDetailId " +
            "AND od.order.deletedAt IS NULL " +
            "AND od.order.orderStatus = 'COMPLETED'")
    Integer getTotalSoldQuantityByProductDetailId(@Param("productDetailId") Integer productDetailId);

    /**
     * Lấy tổng doanh thu theo product_detail_id
     */
    @Query("SELECT SUM(od.finalAmount) FROM OrderDetail od " +
            "WHERE od.productDetail.productDetailId = :productDetailId " +
            "AND od.order.deletedAt IS NULL " +
            "AND od.order.orderStatus = 'COMPLETED'")
    java.math.BigDecimal getTotalRevenueByProductDetailId(@Param("productDetailId") Integer productDetailId);

    /**
     * Đếm số lượng đơn hàng có chứa sản phẩm này
     */
    @Query("SELECT COUNT(DISTINCT od.order.orderId) FROM OrderDetail od " +
            "WHERE od.productDetail.productDetailId = :productDetailId " +
            "AND od.order.deletedAt IS NULL " +
            "AND od.order.orderStatus = 'COMPLETED'")
    Long countOrdersContainingProduct(@Param("productDetailId") Integer productDetailId);
}