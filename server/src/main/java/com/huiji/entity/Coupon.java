package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 优惠券: FULL_CUT 满减 / PERCENT 折扣 / EXPERIENCE 体验 / BIRTHDAY 生日券。
 */
@Entity
@Table(name = "coupon")
@Getter
@Setter
public class Coupon extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /** FULL_CUT / PERCENT / EXPERIENCE / BIRTHDAY */
    @Column(nullable = false)
    private String type;

    /** 面值(分), PERCENT 时为折扣百分比(如 85 表示 8.5 折) */
    @Column(name = "face_value")
    private Long faceValue;

    /** 使用门槛(分) */
    @Column
    private Long threshold = 0L;

    /** DAYS 按天 / RANGE 指定区间 */
    @Column(name = "valid_type")
    private String validType;

    @Column(name = "valid_days")
    private Integer validDays;

    @Column(name = "valid_start")
    private LocalDate validStart;

    @Column(name = "valid_end")
    private LocalDate validEnd;

    /** 发行总量, null 表示不限 */
    @Column
    private Integer total;

    /** 已发放数量 */
    @Column(name = "granted_count")
    private Integer grantedCount = 0;

    /** 已核销数量 */
    @Column(name = "used_count")
    private Integer usedCount = 0;

    /** 每人限领 */
    @Column(name = "per_limit")
    private Integer perLimit = 1;

    /** 适用范围: ALL / STORE */
    @Column
    private String scope = "ALL";

    /** ACTIVE / STOPPED */
    @Column
    private String status = "ACTIVE";
}
