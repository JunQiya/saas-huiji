package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

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
