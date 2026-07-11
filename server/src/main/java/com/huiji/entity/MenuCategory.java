package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 菜单分类(点餐场景的细分类: 热菜/凉菜/饮品等)。
 * 状态: ENABLED / DISABLED。
 */
@Entity
@Table(name = "menu_category", indexes = {
        @Index(name = "idx_mc_tenant_store", columnList = "tenant_id,store_id")
})
@Getter
@Setter
public class MenuCategory extends BaseEntity {

    @Column(name = "store_id")
    private Long storeId;

    @Column(nullable = false)
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private String status = "ENABLED";
}
