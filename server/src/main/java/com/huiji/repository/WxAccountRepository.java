package com.huiji.repository;

import com.huiji.entity.WxAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WxAccountRepository extends JpaRepository<WxAccount, Long> {

    Optional<WxAccount> findByTenantId(Long tenantId);

    Optional<WxAccount> findByTenantIdAndStatus(Long tenantId, String status);

    List<WxAccount> findByAgentIdAndStatus(Long agentId, String status);
}
