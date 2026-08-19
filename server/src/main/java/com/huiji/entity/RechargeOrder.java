package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 储值充值订单: 会员在线充值时先落单, 支付成功(微信回调或演示模拟)后再入账。
 * 状态流转: PENDING(待支付) -> SUCCESS(已到账) / CANCELLED(已关闭)。
 */
@Entity
@Table(name = "recharge_order",
        indexes = {
                @Index(name = "idx_recharge_member", columnList = "member_id"),
                @Index(name = "idx_recharge_out_trade_no", columnList = "out_trade_no"),
                @Index(name = "idx_recharge_status_time", columnList = "tenant_id, status, created_at")
        })
@Getter
@Setter
public class RechargeOrder extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 充值本金(分) */
    @Column(nullable = false)
    private Long amount;

    /** 赠送金额(分), 按租户充值规则匹配 */
    @Column(nullable = false)
    private Long gift = 0L;

    /** PENDING / SUCCESS / CANCELLED */
    @Column(nullable = false)
    private String status = "PENDING";

    /** 商户订单号(唯一) */
    @Column(name = "out_trade_no", nullable = false, unique = true)
    private String outTradeNo;

    /** 微信支付交易号(支付成功后有值) */
    @Column(name = "transaction_id")
    private String transactionId;

    /** 支付方式: WECHAT / BALANCE / CASH */
    @Column(name = "pay_method")
    private String payMethod;

    /** 入账后余额(分) */
    @Column(name = "balance_after")
    private Long balanceAfter;

    @Column(name = "paid_at")
    private java.time.LocalDateTime paidAt;
}
