package com.huiji.entity;

import com.huiji.entity.converter.LongListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 会员 */
@Entity
@Table(name = "member")
@Getter
@Setter
public class Member extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    /** MALE / FEMALE / UNKNOWN */
    @Column
    private String gender = "UNKNOWN";

    @Column
    private LocalDate birthday;

    /** 等级序号 1..N */
    @Column(name = "level_code")
    private Integer level = 1;

    /** 储值余额(分) */
    @Column(nullable = false)
    private Long balance = 0L;

    /** 积分 */
    @Column(nullable = false)
    private Long points = 0L;

    @Convert(converter = LongListConverter.class)
    @Column(name = "store_ids")
    private List<Long> storeIds = new ArrayList<>();

    /** 累计消费次数 */
    @Column(name = "consume_count")
    private Integer consumeCount = 0;

    /** 累计消费金额(分) */
    @Column(name = "total_amount")
    private Long totalAmount = 0L;

    @Column(name = "last_consume_at")
    private LocalDateTime lastConsumeAt;

    @Column
    private String remark;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /** 微信 openid */
    @Column(name = "wx_openid")
    private String wxOpenid;

    /** 微信头像 */
    @Column(name = "wx_head_img_url", length = 512)
    private String wxHeadImgUrl;
}
