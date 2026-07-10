package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 收银订单(收银台/前台下单/服务履约单据)。
 * 状态机: PENDING -> PAID -> REFUNDED, 或 PENDING -> VOID。
 */
@Entity
@Table(name = "sales_order", indexes = {
        @Index(name = "idx_order_tenant", columnList = "tenant_id"),
        @Index(name = "idx_order_no", columnList = "order_no", unique = true),
        @Index(name = "idx_order_member", columnList = "member_id"),
        @Index(name = "idx_order_store", columnList = "store_id")
})
@Getter
@Setter
public class Order extends BaseEntity {

    @Column(name = "order_no", nullable = false, length = 64)
    private String orderNo;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "cashier_id")
    private Long cashierId;

    /** 订单总额(分) */
    @Column(name = "total_amount", nullable = false)
    private Long totalAmount = 0L;

    /** 优惠金额(分) */
    @Column(name = "discount_amount")
    private Long discountAmount = 0L;

    /** 实收金额(分) */
    @Column(name = "paid_amount")
    private Long paidAmount = 0L;

    /** CASH / WECHAT / ALIPAY / BALANCE / MIXED */
    @Column(name = "pay_method")
    private String payMethod;

    /** PENDING / PAID / REFUNDED / VOID */
    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_reason")
    private String refundReason;

    @Column
    private String remark;
}
