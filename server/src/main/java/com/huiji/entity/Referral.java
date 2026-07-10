package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 会员推荐关系: A 推荐了 B, 产生一条 Referral 记录。
 * 不在 Member 上加字段, code 在 Referral 冗余存储。
 */
@Entity
@Table(name = "referral", indexes = {
        @Index(name = "idx_ref_referrer", columnList = "referrer_id"),
        @Index(name = "idx_ref_code", columnList = "code")
})
@Getter
@Setter
public class Referral extends BaseEntity {

    /** 推荐人 memberId */
    @Column(name = "referrer_id", nullable = false)
    private Long referrerId;

    /** 被推荐人 memberId */
    @Column(name = "referee_id", nullable = false)
    private Long refereeId;

    @Column(name = "referee_name")
    private String refereeName;

    @Column(name = "referee_phone")
    private String refereePhone;

    /** 推荐码(冗余, 便于反查) */
    @Column(length = 32)
    private String code;

    /** REGISTERED / ACTIVE / REWARDED */
    @Column(nullable = false)
    private String status = "REGISTERED";

    /** 奖励金额(分) */
    @Column(name = "reward_amount", nullable = false)
    private Long rewardAmount = 0L;

    /** COUPON / BALANCE */
    @Column(name = "reward_type")
    private String rewardType;

    /** 关联优惠券记录或流水 id */
    @Column(name = "reward_id")
    private Long rewardId;
}
