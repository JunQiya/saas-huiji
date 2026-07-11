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
 * 赢奖小游戏: 大转盘/刮刮乐/砸金蛋/摇一摇。
 */
@Entity
@Table(name = "game", indexes = {
        @Index(name = "idx_game_tenant", columnList = "tenant_id"),
        @Index(name = "idx_game_store", columnList = "store_id")
})
@Getter
@Setter
public class Game extends BaseEntity {

    /** 关联门店, null 表示全部门店 */
    @Column(name = "store_id")
    private Long storeId;

    @Column(nullable = false)
    private String name;

    /** WHEEL 大转盘 / SCRATCH 刮刮乐 / EGG 砸金蛋 / SHAKE 摇一摇 */
    @Column(nullable = false)
    private String type;

    @Column
    private String subtitle;

    @Column(name = "cover_image")
    private String coverImage;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    /** 每日次数限制, 默认 1 */
    @Column(name = "daily_limit", nullable = false)
    private Integer dailyLimit = 1;

    /** 总次数限制, 0 表示不限 */
    @Column(name = "total_limit", nullable = false)
    private Integer totalLimit = 0;

    /** 每次消耗积分, 默认 0 */
    @Column(name = "points_cost", nullable = false)
    private Integer pointsCost = 0;

    /** ENABLED / DISABLED */
    @Column(nullable = false)
    private String status = "ENABLED";

    /** 规则说明 */
    @Lob
    @Column
    private String rules;

    @Column(name = "bg_image")
    private String bgImage;
}
