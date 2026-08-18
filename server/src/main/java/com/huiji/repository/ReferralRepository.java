package com.huiji.repository;

import com.huiji.entity.Referral;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    Optional<Referral> findByRefereeIdAndDeletedFalse(Long refereeId);

    /** 按邀请码反查推荐人记录：同一邀请码可能出现在多条被绑记录上，取最早一条（本人占位记录） */
    Optional<Referral> findFirstByCodeAndDeletedFalse(String code);

    List<Referral> findByReferrerIdAndDeletedFalseOrderByIdDesc(Long referrerId);

    @Query("select r from Referral r where r.tenantId = :tenantId and r.deleted = false " +
            "and (:referrerId is null or r.referrerId = :referrerId) order by r.id desc")
    Page<Referral> search(@Param("tenantId") Long tenantId,
                          @Param("referrerId") Long referrerId,
                          Pageable pageable);

    long countByReferrerIdAndDeletedFalse(Long referrerId);

    long countByReferrerIdAndStatusAndDeletedFalse(Long referrerId, String status);

    long countByTenantIdAndDeletedFalse(Long tenantId);

    long countByTenantIdAndStatusAndDeletedFalse(Long tenantId, String status);

    long countByTenantIdAndCreatedAtAfterAndDeletedFalse(Long tenantId, java.time.LocalDateTime after);

    @Query("select coalesce(sum(r.rewardAmount),0) from Referral r where r.tenantId = :tenantId and r.deleted = false")
    Long sumRewardByTenant(@Param("tenantId") Long tenantId);

    @Query("select coalesce(sum(r.rewardAmount),0) from Referral r where r.referrerId = :referrerId " +
            "and r.deleted = false")
    Long sumReward(@Param("referrerId") Long referrerId);
}
