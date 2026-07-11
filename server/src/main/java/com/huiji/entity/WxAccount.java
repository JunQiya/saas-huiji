package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 微信公众号配置: 每个租户可配置自己的公众号, 也可挂靠代理商。
 */
@Entity
@Table(name = "wx_account", indexes = {
        @Index(name = "idx_wx_account_tenant", columnList = "tenant_id")
})
@Getter
@Setter
public class WxAccount extends BaseEntity {

    /** 代理商 ID, 可空(表示未挂靠代理商) */
    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "app_id")
    private String appId;

    @Column(name = "app_secret")
    private String appSecret;

    @Column(name = "mch_id")
    private String mchId;

    @Column(name = "mch_key")
    private String mchKey;

    @Column(name = "api_v3_key")
    private String apiV3Key;

    @Column(name = "cert_path")
    private String certPath;

    /**
     * 模板消息 ID JSON, 形如:
     * {"login":"xxx","recharge":"xxx","order_paid":"xxx"}
     */
    @Lob
    @Column(name = "template_ids")
    private String templateIds;

    /** 授权回调域名, 如 https://shop.example.com */
    @Column(name = "domain")
    private String domain;

    /** ENABLED / DISABLED */
    @Column(nullable = false)
    private String status = "ENABLED";
}
