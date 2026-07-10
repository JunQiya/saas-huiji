package com.huiji.repository;

import com.huiji.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    /**
     * 销量 Top N, 返回 [productId, productName, SUM(quantity), SUM(subtotal)]。
     * 限定时间区间: start/end 任一为空时不限。通过 Pageable 控制返回条数。
     */
    @Query("SELECT oi.productId, oi.productName, SUM(oi.quantity), SUM(oi.subtotal) " +
            "FROM OrderItem oi " +
            "WHERE oi.tenantId = :tenantId " +
            "AND (:start IS NULL OR oi.createdAt >= :start) " +
            "AND (:end IS NULL OR oi.createdAt < :end) " +
            "GROUP BY oi.productId, oi.productName " +
            "ORDER BY SUM(oi.quantity) DESC, SUM(oi.subtotal) DESC")
    List<Object[]> topProductsInRange(@Param("tenantId") Long tenantId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      Pageable pageable);
}
