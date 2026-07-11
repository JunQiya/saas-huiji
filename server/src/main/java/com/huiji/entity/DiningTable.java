package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 餐桌(线下门店点餐)。
 * 状态: IDLE 空闲 / OCCUPIED 占用 / RESERVED 预订。
 */
@Entity
@Table(name = "dining_table", indexes = {
        @Index(name = "idx_dt_tenant_store", columnList = "tenant_id,store_id"),
        @Index(name = "idx_dt_qrcode", columnList = "qrcode")
})
@Getter
@Setter
public class DiningTable extends BaseEntity {

    @Column(name = "store_id")
    private Long storeId;

    @Column(nullable = false)
    private String name;

    @Column
    private String area;

    @Column
    private Integer seats;

    @Column(nullable = false)
    private String status = "IDLE";

    @Column
    private String qrcode;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
