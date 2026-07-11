package com.huiji.repository;

import com.huiji.entity.KitchenOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KitchenOrderRepository extends JpaRepository<KitchenOrder, Long> {

    List<KitchenOrder> findByTenantIdAndStoreIdAndStatusAndDeletedFalseOrderByCreatedAtAsc(Long tenantId, Long storeId, String status);

    List<KitchenOrder> findByTenantIdAndStoreIdAndDeletedFalseOrderByCreatedAtDesc(Long tenantId, Long storeId);

    Optional<KitchenOrder> findByOrderId(Long orderId);

    Optional<KitchenOrder> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    @Query("select k from KitchenOrder k where k.tenantId = :tenantId and k.deleted = false " +
            "and k.orderId in (select o.id from Order o where o.memberId = :memberId) " +
            "order by k.createdAt desc")
    List<KitchenOrder> findByTenantIdAndMemberId(@Param("tenantId") Long tenantId,
                                                  @Param("memberId") Long memberId);
}
