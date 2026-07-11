package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 代理商: 可配置默认公众号供挂靠商家使用, 并参与抽佣。
 */
@Entity
@Table(name = "agent")
@Getter
@Setter
public class Agent extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_phone")
    private String contactPhone;

    /** 代理商公众号 AppId, 给挂靠商家用 */
    @Column(name = "app_id")
    private String appId;

    @Column(name = "app_secret")
    private String appSecret;

    @Column(name = "mch_id")
    private String mchId;

    @Column(name = "mch_key")
    private String mchKey;

    /** 抽佣比例(千分比, 如 50 表示 5%) */
    @Column(name = "commission_rate")
    private Integer commissionRate = 0;

    /** ENABLED / DISABLED */
    @Column(nullable = false)
    private String status = "ENABLED";
}
