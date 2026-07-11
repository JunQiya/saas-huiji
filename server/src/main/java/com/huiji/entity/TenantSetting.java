package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 租户设置: 等级规则、储值规则等(JSON 字符串存储)。
 */
@Entity
@Table(name = "tenant_setting")
@Getter
@Setter
public class TenantSetting extends BaseEntity {

    @Column(name = "tenant_name")
    private String tenantName;

    @Column(name = "brand_color")
    private String brandColor;

    @Column(name = "sms_sign")
    private String smsSign;

    /** 当前套餐 FREE/BASIC/GROWTH/FLAGSHIP */
    @Column(name = "plan_code")
    private String plan = "FREE";

    /** 套餐到期时间 */
    @Column(name = "plan_expires_at")
    private LocalDateTime planExpiresAt;

    /** 短信余额 */
    @Column(name = "sms_balance")
    private Integer smsBalance = 0;

    /** 功能开关 JSON，存储各功能模块的启用状态 */
    @Lob
    @Column(name = "feature_flags")
    private String featureFlags;

    /**
     * 等级规则 JSON, 形如:
     * [{"level":1,"name":"普通会员","threshold":0},{"level":2,"name":"银卡","threshold":50000},...]
     */
    @Lob
    @Column(name = "level_rules")
    private String levelRules;

    /**
     * 储值赠送规则 JSON, 形如:
     * [{"recharge":10000,"gift":1000},...]
     */
    @Lob
    @Column(name = "recharge_rules")
    private String rechargeRules;
}
