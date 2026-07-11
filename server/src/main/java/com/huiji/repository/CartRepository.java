package com.huiji.repository;

import com.huiji.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /** 会员购物车列表 */
    List<Cart> findByMemberIdAndTenantIdAndDeletedFalseOrderByIdDesc(Long memberId, Long tenantId);

    /** 会员选中的购物车项 */
    List<Cart> findByMemberIdAndTenantIdAndSelectedAndDeletedFalse(Long memberId, Long tenantId, Boolean selected);

    /** 查会员某商品的购物车项(用于加购去重) */
    Optional<Cart> findByMemberIdAndTenantIdAndProductIdAndDeletedFalse(Long memberId, Long tenantId, Long productId);

    /** 移除会员某商品的购物车项 */
    @Transactional
    void deleteByMemberIdAndTenantIdAndProductId(Long memberId, Long tenantId, Long productId);

    /** 清空会员购物车 */
    @Transactional
    void deleteByMemberIdAndTenantId(Long memberId, Long tenantId);
}
