package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 厨房工单(扫码点餐后生成的后厨打单)。
 * orderType: DINE_IN 堂食 / TAKEOUT 外带。
 * 状态: PENDING 待处理 -> COOKING 烹饪中 -> SERVED 已上餐; CANCELLED 已取消。
 * items 以 JSON 存储: [{productId,name,quantity,remark}]。
 */
@Entity
@Table(name = "kitchen_order", indexes = {
        @Index(name = "idx_ko_tenant_store", columnList = "tenant_id,store_id"),
        @Index(name = "idx_ko_order", columnList = "order_id")
})
@Getter
@Setter
public class KitchenOrder extends BaseEntity {

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "order_type")
    private String orderType;

    @Column(nullable = false)
    private String status = "PENDING";

    @Lob
    @Column
    private String items;

    @Column(name = "served_at")
    private LocalDateTime servedAt;
}
