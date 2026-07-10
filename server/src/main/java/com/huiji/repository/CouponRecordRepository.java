package com.huiji.repository;

import com.huiji.entity.CouponRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRecordRepository extends JpaRepository<CouponRecord, Long> {

    Optional<CouponRecord> findByCodeAndTenantIdAndDeletedFalse(String code, Long tenantId);

    @Query("select r from CouponRecord r where r.tenantId = :tenantId and r.couponId = :couponId " +
            "and r.deleted = false order by r.id desc")
    List<CouponRecord> findByCoupon(@Param("tenantId") Long tenantId, @Param("couponId") Long couponId);

    @Query("select r from CouponRecord r where r.tenantId = :tenantId and r.memberId = :memberId " +
            "and (:status is null or :status = '' or r.status = :status) and r.deleted = false order by r.id desc")
    List<CouponRecord> findByMember(@Param("tenantId") Long tenantId,
                                    @Param("memberId") Long memberId,
                                    @Param("status") String status);

    long countByTenantIdAndCouponIdAndMemberIdAndDeletedFalse(Long tenantId, Long couponId, Long memberId);

    long countByTenantIdAndCouponIdAndDeletedFalse(Long tenantId, Long couponId);
}
