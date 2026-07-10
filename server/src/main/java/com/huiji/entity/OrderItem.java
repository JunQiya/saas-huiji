package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 订单明细: 售出商品/服务快照, 防止商品改名/改价后历史单据失真。
 */
@Entity
@Table(name = "sales_order_item", indexes = @Index(name = "idx_oi_order", columnList = "order_id"))
@Getter
@Setter
public class OrderItem extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id")
    private Long productId;

    /** 售出时商品名称快照 */
    @Column(name = "product_name", nullable = false)
    private String productName;

    /** 售出时单价(分) */
    @Column(name = "unit_price", nullable = false)
    private Long unitPrice = 0L;

    @Column(nullable = false)
    private Integer quantity = 1;

    /** 小计(分) = unitPrice * quantity */
    @Column(nullable = false)
    private Long subtotal = 0L;
}
