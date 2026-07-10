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

    Optional<Referral> findByCodeAndDeletedFalse(String code);

    List<Referral> findByReferrerIdAndDeletedFalseOrderByIdDesc(Long referrerId);

    @Query("select r from Referral r where r.tenantId = :tenantId and r.deleted = false " +
            "and (:referrerId is null or r.referrerId = :referrerId) order by r.id desc")
    Page<Referral> search(@Param("tenantId") Long tenantId,
                          @Param("referrerId") Long referrerId,
                          Pageable pageable);

    long countByReferrerIdAndDeletedFalse(Long referrerId);

    long countByReferrerIdAndStatusAndDeletedFalse(Long referrerId, String status);

    @Query("select coalesce(sum(r.rewardAmount),0) from Referral r where r.referrerId = :referrerId " +
            "and r.deleted = false")
    Long sumReward(@Param("referrerId") Long referrerId);
}
