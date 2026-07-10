package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 营销活动触发日志 */
@Entity
@Table(name = "campaign_log", indexes = @Index(name = "idx_campaign_log_camp", columnList = "campaign_id"))
@Getter
@Setter
public class CampaignLog extends BaseEntity {

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "campaign_name")
    private String campaignName;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "member_name")
    private String memberName;

    /** TRIGGERED / REACHED / CONVERTED */
    @Column(nullable = false)
    private String action;

    @Column
    private String detail;
}
