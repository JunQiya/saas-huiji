package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 商城分类: 商城商品的一级分组, 租户隔离。
 */
@Entity
@Table(name = "mall_category", indexes = {
        @Index(name = "idx_mall_category_tenant", columnList = "tenant_id")
})
@Getter
@Setter
public class MallCategory extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /** 图标 URL, 可空 */
    @Column
    private String icon;

    /** 排序值, 越小越靠前 */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /** ENABLED / DISABLED */
    @Column(nullable = false)
    private String status = "ENABLED";

    /** 分类下商品数(冗余字段, 可空) */
    @Column(name = "product_count")
    private Integer productCount = 0;
}
