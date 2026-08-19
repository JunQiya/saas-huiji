package com.huiji.repository;

import com.huiji.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Optional<Campaign> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    long countByTenantIdAndDeletedFalse(Long tenantId);

    @Query("select c from Campaign c where c.tenantId = :tenantId and c.deleted = false " +
            "and (:status is null or :status = '' or " +
            "(:status = 'ENABLED' and c.enabled = true) or " +
            "(:status = 'DISABLED' and c.enabled = false)) order by c.id desc")
    List<Campaign> listByTenant(@Param("tenantId") Long tenantId, @Param("status") String status);
}
