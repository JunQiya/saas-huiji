package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 消息发送任务: 短信/微信/站内触达。
 * memberIds 以 JSON 字符串数组保存; cost 单位为分(对账方便)。
 */
@Entity
@Table(name = "message_task")
@Getter
@Setter
public class MessageTask extends BaseEntity {

    /** SMS / WECHAT / IN_APP */
    @Column(nullable = false)
    private String channel;

    /** BIRTHDAY / COUPON_EXPIRE / CAMPAIGN / MANUAL */
    @Column(nullable = false)
    private String templateType;

    @Column
    private String subject;

    @Lob
    @Column
    private String content;

    /** 目标会员 id 列表(JSON 数组字符串) */
    @Lob
    @Column(name = "member_ids")
    private String memberIds;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount = 0;

    @Column(name = "sent_count", nullable = false)
    private Integer sentCount = 0;

    @Column(name = "failed_count", nullable = false)
    private Integer failedCount = 0;

    /** 费用(分) */
    @Column(nullable = false)
    private Long cost = 0L;

    /** PENDING / SENDING / COMPLETED / FAILED / CANCELED */
    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_by")
    private Long createdBy;
}
