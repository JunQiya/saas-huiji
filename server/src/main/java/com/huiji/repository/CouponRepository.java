package com.huiji.repository;

import com.huiji.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    @Query("select c from Coupon c where c.tenantId = :tenantId and c.deleted = false " +
            "and (:status is null or :status = '' or c.status = :status) " +
            "and (:type is null or :type = '' or c.type = :type) order by c.id desc")
    List<Coupon> listByTenant(@Param("tenantId") Long tenantId,
                              @Param("status") String status,
                              @Param("type") String type);

    /** H5 可领取的券(ACTIVE 且未停用) */
    @Query("select c from Coupon c where c.tenantId = :tenantId and c.deleted = false " +
            "and c.status = 'ACTIVE' order by c.id desc")
    List<Coupon> listClaimable(@Param("tenantId") Long tenantId);
}
