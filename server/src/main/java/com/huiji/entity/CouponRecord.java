package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 券发放/核销记录: UNUSED 未用 / USED 已用 / EXPIRED 已过期。
 */
@Entity
@Table(name = "coupon_record", indexes = {
        @Index(name = "idx_coupon_record_member", columnList = "member_id"),
        @Index(name = "idx_coupon_record_coupon", columnList = "coupon_id")
})
@Getter
@Setter
public class CouponRecord extends BaseEntity {

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "coupon_name")
    private String couponName;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "member_name")
    private String memberName;

    /** 核销码 */
    @Column(nullable = false)
    private String code;

    /** UNUSED / USED / EXPIRED */
    @Column(nullable = false)
    private String status = "UNUSED";

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "used_store_id")
    private Long usedStoreId;
}
