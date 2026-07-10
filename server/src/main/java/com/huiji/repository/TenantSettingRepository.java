package com.huiji.repository;

import com.huiji.entity.TenantSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantSettingRepository extends JpaRepository<TenantSetting, Long> {

    Optional<TenantSetting> findByTenantId(Long tenantId);
}
