package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 游戏奖品配置: COUPON 优惠券 / POINTS 积分 / EMPTY 谢谢参与。
 */
@Entity
@Table(name = "game_prize", indexes = {
        @Index(name = "idx_gprize_game", columnList = "game_id")
})
@Getter
@Setter
public class GamePrize extends BaseEntity {

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private String name;

    /** COUPON / POINTS / EMPTY */
    @Column(nullable = false)
    private String type;

    /** 关联 ID, 如优惠券 ID */
    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "ref_name")
    private String refName;

    /** 积分数量或优惠券数量 */
    @Column
    private Integer amount;

    /** 中奖概率千分比, 如 50 表示 5% */
    @Column(nullable = false)
    private Integer probability = 0;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
