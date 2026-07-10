package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 报表订阅任务: 按计划自动生成 PDF/Excel 并邮件发送。
 *  - type: DASHBOARD / REVENUE / MEMBER / COUPON / ORDER
 *  - schedule: DAILY / WEEKLY / MONTHLY / ONCE
 *  - recipients: 逗号分隔邮箱
 */
@Entity
@Table(name = "report_task")
@Getter
@Setter
public class ReportTask extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /** DASHBOARD / REVENUE / MEMBER / COUPON / ORDER */
    @Column(nullable = false)
    private String type;

    /** DAILY / WEEKLY / MONTHLY / ONCE */
    @Column(nullable = false)
    private String schedule;

    @Lob
    @Column
    private String recipients;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(nullable = false)
    private Boolean enabled = true;
}
