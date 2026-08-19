package com.huiji.repository;

import com.huiji.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, Long> {

    List<Agent> findByTenantIdAndDeletedFalseOrderByIdDesc(Long tenantId);

    long countByTenantIdAndDeletedFalse(Long tenantId);

    Optional<Agent> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    Optional<Agent> findByIdAndStatusAndDeletedFalse(Long id, String status);
}
