package com.huiji.repository;

import com.huiji.entity.CampaignLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignLogRepository extends JpaRepository<CampaignLog, Long> {

    List<CampaignLog> findByTenantIdAndCampaignIdOrderByIdDesc(Long tenantId, Long campaignId);
}
