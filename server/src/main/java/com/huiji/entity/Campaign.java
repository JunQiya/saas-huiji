package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 营销活动: BIRTHDAY 生日 / DORMANT 唤醒沉睡 / REPURCHASE 复购 / MANUAL 手动。
 */
@Entity
@Table(name = "campaign")
@Getter
@Setter
public class Campaign extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /** BIRTHDAY / DORMANT / REPURCHASE / MANUAL */
    @Column(nullable = false)
    private String type;

    /** 触发条件描述(如: 生日前3天 / 30天未到店) */
    @Column
    private String trigger;

    /** 目标人群描述(JSON 字符串, 如 level/tag/storeIds) */
    @Column
    private String audience;

    /** SMS / WECHAT / IN_APP */
    @Column(nullable = false)
    private String channel;

    /** 触达内容(文案模板) */
    @Lob
    @Column
    private String content;

    /** SOP 步骤(JSON 字符串) */
    @Lob
    @Column(name = "sop_steps")
    private String sopSteps;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(nullable = false)
    private Boolean enabled = true;

    /** 统计: 触发人数 */
    @Column(name = "stat_triggered")
    private Integer statTriggered = 0;

    /** 统计: 触达人数 */
    @Column(name = "stat_reached")
    private Integer statReached = 0;

    /** 统计: 转化人数 */
    @Column(name = "stat_converted")
    private Integer statConverted = 0;
}
