package com.huiji.repository;

import com.huiji.entity.MallCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MallCategoryRepository extends JpaRepository<MallCategory, Long> {

    /** 分类列表(按 sortOrder 升序) */
    List<MallCategory> findByTenantIdOrderBySortOrderAsc(Long tenantId);

    /** 按状态筛选 */
    List<MallCategory> findByTenantIdAndStatusOrderBySortOrderAsc(Long tenantId, String status);
}
