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
     * 商品/服务列表搜索: 关键字/分类/状态。
     * storeId 过滤在 Service 层内存中处理(避开 List 类型 JPQL coalesce 限制)。
     * keyword 同时匹配 name 与 description。
     */
    @Query("select p from Product p where p.tenantId = :tenantId and p.deleted = false " +
            "and (:keyword is null or :keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')) " +
            "    or lower(coalesce(p.description, '')) like lower(concat('%', :keyword, '%'))) " +
            "and (:category is null or :category = '' or p.category = :category) " +
            "and (:status is null or :status = '' or p.status = :status) " +
            "order by p.id desc")
    Page<Product> search(@Param("tenantId") Long tenantId,
                         @Param("keyword") String keyword,
                         @Param("category") String category,
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

    /** 按菜单分类查上架商品 */
    List<Product> findByTenantIdAndMenuCategoryIdAndStatusAndDeletedFalseOrderByIdAsc(
            Long tenantId, Long menuCategoryId, String status);

    /** 未关联菜单分类的商品(用于点餐菜单数据补全) */
    List<Product> findByTenantIdAndMenuCategoryIdNullAndDeletedFalseOrderByIdAsc(Long tenantId);

    /** 按 ID 批量查商品(绑定分类用) */
    List<Product> findByIdInAndTenantIdAndDeletedFalse(List<Long> ids, Long tenantId);

    /** 商城分类下上架商品 */
    List<Product> findByTenantIdAndMallCategoryIdAndMallVisibleTrueAndDeletedFalseOrderByIdAsc(
            Long tenantId, Long mallCategoryId);

    /** 商城所有上架商品 */
    List<Product> findByTenantIdAndMallVisibleTrueAndDeletedFalseOrderByIdDesc(Long tenantId);

    /** 商城商品搜索(关键字+分类, 分页) */
    @Query("select p from Product p where p.tenantId = :tenantId and p.deleted = false " +
            "and p.mallVisible = true and p.status = 'ACTIVE' " +
            "and (:categoryId is null or p.mallCategoryId = :categoryId) " +
            "and (:keyword is null or :keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%')) " +
            "    or lower(coalesce(p.description, '')) like lower(concat('%', :keyword, '%'))) " +
            "order by p.id desc")
    Page<Product> searchMall(@Param("tenantId") Long tenantId,
                             @Param("categoryId") Long categoryId,
                             @Param("keyword") String keyword,
                             Pageable pageable);
}
