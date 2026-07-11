package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * 购物车: 会员加购记录, (memberId, productId) 唯一, 同一商品追加数量。
 */
@Entity
@Table(name = "cart", indexes = {
        @Index(name = "idx_cart_member", columnList = "tenant_id,member_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_cart_member_product", columnNames = {"member_id", "product_id"})
})
@Getter
@Setter
public class Cart extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** 商品名称快照 */
    @Column(name = "product_name")
    private String productName;

    /** 商品图片快照 */
    @Column(name = "product_image")
    private String productImage;

    /** 加购时价格(分) */
    @Column(nullable = false)
    private Long price = 0L;

    @Column(nullable = false)
    private Integer quantity = 1;

    /** 是否选中, 默认 true */
    @Column(nullable = false)
    private Boolean selected = true;
}
