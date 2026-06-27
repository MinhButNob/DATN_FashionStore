package com.example.backend_fashionstore.QuanLySanPham.repository.Product;

import com.example.backend_fashionstore.entity.Product;
import com.example.backend_fashionstore.entity.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

    @Query("SELECT p FROM Product p WHERE p.productId = :id AND p.deletedAt IS NULL")
    Optional<Product> findByIdAndNotDeleted(@Param("id") Integer id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.productDetails pd WHERE p.productId = :id AND p.deletedAt IS NULL AND pd.deletedAt IS NULL")
    Optional<Product> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.deletedAt IS NULL AND p.status = 'ACTIVE'")
    long countActiveProducts();

    @Query("SELECT COUNT(pd) FROM ProductDetail pd WHERE pd.deletedAt IS NULL AND pd.status = 'ACTIVE' AND pd.quantity > 0")
    long countInStockVariants();

    @Query("SELECT COUNT(pd) FROM ProductDetail pd WHERE pd.deletedAt IS NULL AND pd.status = 'ACTIVE' AND pd.quantity > 0 AND pd.quantity <= :threshold")
    long countLowStockVariants(@Param("threshold") int threshold);

    @Query("SELECT pd FROM ProductDetail pd JOIN FETCH pd.product p WHERE pd.deletedAt IS NULL AND pd.status = 'ACTIVE' AND pd.quantity > 0 AND pd.quantity <= :threshold ORDER BY pd.quantity ASC")
    List<ProductDetail> findLowStockVariants(@Param("threshold") int threshold);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.deletedAt = :now, p.status = 'DELETED' WHERE p.productId = :id AND p.deletedAt IS NULL")
    int softDelete(@Param("id") Integer id, @Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.productDetails pd " +
            "LEFT JOIN FETCH pd.color " +
            "LEFT JOIN FETCH pd.size " +
            "WHERE p.productId IN :ids AND p.deletedAt IS NULL")
    List<Product> findAllWithDetailsByIds(@Param("ids") List<Integer> ids);

    @Query("SELECT p FROM Product p WHERE p.productId = :id")
    Optional<Product> findById(@Param("id") Integer id);

    @Query("SELECT p FROM Product p WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.status = 'ACTIVE' AND p.deletedAt IS NULL")
    List<Product> searchActiveProductsByKeyword(@Param("keyword") String keyword);
}