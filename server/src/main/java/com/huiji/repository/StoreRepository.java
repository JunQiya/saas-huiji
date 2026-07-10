package com.huiji.repository;

import com.huiji.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByTenantIdAndDeletedFalseOrderByIdDesc(Long tenantId);

    long countByTenantIdAndDeletedFalse(Long tenantId);
}
