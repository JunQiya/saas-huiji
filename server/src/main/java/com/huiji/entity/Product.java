package com.huiji.entity;

import com.huiji.entity.converter.LongListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品/服务: 门店销售的基础单位。
 *  - category: SERVICE 服务 / GOODS 商品
 *  - stock: 仅 GOODS 有效(SERVICE 留空)
 *  - storeIds: 适用门店(空表示全店)
 */
@Entity
@Table(name = "product", indexes = {
        @Index(name = "idx_product_tenant", columnList = "tenant_id"),
        @Index(name = "idx_product_category", columnList = "category")
})
@Getter
@Setter
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /** SERVICE / GOODS */
    @Column(nullable = false)
    private String category = "SERVICE";

    /** 封面图 URL */
    @Column
    private String cover;

    /** 售价(分) */
    @Column(nullable = false)
    private Long price = 0L;

    /** 成本价(分), 仅内部参考 */
    @Column(name = "cost_price")
    private Long costPrice = 0L;

    /** 库存, 仅 GOODS 有效 */
    @Column
    private Integer stock;

    /** ACTIVE / DISABLED */
    @Column(nullable = false)
    private String status = "ACTIVE";

    /** 累计售出 */
    @Column(name = "sold_count")
    private Integer soldCount = 0;

    @Column
    private String description;

    @Convert(converter = LongListConverter.class)
    @Column(name = "store_ids")
    private List<Long> storeIds = new ArrayList<>();
}
