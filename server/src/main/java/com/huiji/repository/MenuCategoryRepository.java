package com.huiji.repository;

import com.huiji.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    List<MenuCategory> findByTenantIdAndStoreIdAndDeletedFalseOrderBySortOrderAscIdAsc(Long tenantId, Long storeId);

    List<MenuCategory> findByTenantIdAndDeletedFalseOrderBySortOrderAscIdAsc(Long tenantId);

    Optional<MenuCategory> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);
}
