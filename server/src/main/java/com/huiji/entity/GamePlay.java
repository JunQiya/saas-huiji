package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 游戏参与记录: 每次玩游戏产生一条。
 */
@Entity
@Table(name = "game_play", indexes = {
        @Index(name = "idx_gplay_game_member", columnList = "game_id,member_id"),
        @Index(name = "idx_gplay_day", columnList = "tenant_id,game_id,member_id,day_key")
})
@Getter
@Setter
public class GamePlay extends BaseEntity {

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "prize_id")
    private Long prizeId;

    @Column(name = "prize_name")
    private String prizeName;

    /** COUPON / POINTS / EMPTY */
    @Column(name = "prize_type")
    private String prizeType;

    @Column(name = "is_win", nullable = false)
    private Boolean isWin = false;

    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt;

    /** 当天日期字符串, 如 2026-07-11, 用于按日统计 */
    @Column(name = "day_key", nullable = false)
    private String dayKey;
}
