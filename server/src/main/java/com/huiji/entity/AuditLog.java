package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 操作审计日志 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLog extends BaseEntity {

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_name")
    private String operatorName;

    /** 动作描述(如 会员充值/券核销) */
    @Column(nullable = false)
    private String action;

    /** 操作目标(如 member:12) */
    @Column
    private String target;

    @Lob
    @Column
    private String detail;

    @Column
    private String ip;
}
