package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 储值流水: RECHARGE 充值 / CONSUME 消费 / REFUND 退款 / GIFT 赠送。
 */
@Entity
@Table(name = "wallet_transaction", indexes = @Index(name = "idx_wallet_member", columnList = "member_id"))
@Getter
@Setter
public class WalletTransaction extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** RECHARGE / CONSUME / REFUND / GIFT */
    @Column(nullable = false)
    private String type;

    /** 变动金额(分), 充值为正, 消费为负 */
    @Column(nullable = false)
    private Long amount;

    /** 赠送金额(分), 仅充值时有 */
    @Column
    private Long gift = 0L;

    /** 变动后余额(分) */
    @Column(name = "balance_after")
    private Long balanceAfter;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "operator_id")
    private Long operatorId;

    /** 支付方式: CASH / WECHAT / ALIPAY / BALANCE / CARD */
    @Column(name = "pay_method")
    private String payMethod;

    @Column
    private String remark;

    /** 关联订单号(消费时) */
    @Column(name = "order_no")
    private String orderNo;
}
