package com.huiji.repository;

import com.huiji.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    List<DiningTable> findByTenantIdAndStoreIdAndDeletedFalseOrderBySortOrderAscIdAsc(Long tenantId, Long storeId);

    List<DiningTable> findByTenantIdAndStoreIdAndStatusAndDeletedFalse(Long tenantId, Long storeId, String status);

    Optional<DiningTable> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    Optional<DiningTable> findByQrcodeAndDeletedFalse(String qrcode);
}
