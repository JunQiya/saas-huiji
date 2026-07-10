package com.huiji.repository;

import com.huiji.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    /**
     * 商品/服务列表搜索: 关键字/分类/门店/状态。
     * storeId 为字符串形式, 使用 like 匹配逗号分隔的 storeIds。
     * keyword 同时匹配 name 与 description。
     */
    @Query("select p from Product p where p.tenantId = :tenantId and p.deleted = false " +
            "and (:keyword is null or :keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')) " +
            "    or lower(coalesce(p.description, '')) like lower(concat('%', :keyword, '%'))) " +
            "and (:category is null or :category = '' or p.category = :category) " +
            "and (:storeId is null or :storeId = '' or coalesce(p.storeIds, '') like concat('%', :storeId, '%')) " +
            "and (:status is null or :status = '' or p.status = :status) " +
            "order by p.id desc")
    Page<Product> search(@Param("tenantId") Long tenantId,
                         @Param("keyword") String keyword,
                         @Param("category") String category,
                         @Param("storeId") String storeId,
                         @Param("status") String status,
                         Pageable pageable);

    /** 取上架商品(SERVICE/GOODS), 收银台拉商品网格用 */
    @Query("select p from Product p where p.tenantId = :tenantId and p.deleted = false and p.status = 'ACTIVE' " +
            "and (:category is null or :category = '' or p.category = :category) " +
            "order by p.id asc")
    List<Product> listActive(@Param("tenantId") Long tenantId,
                             @Param("category") String category);

    /** 套餐配额统计 */
    long countByTenantIdAndDeletedFalse(Long tenantId);
}
