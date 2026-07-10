package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 登录日志 */
@Entity
@Table(name = "login_log")
@Getter
@Setter
public class LoginLog extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column
    private String username;

    @Column
    private String ip;

    /** SUCCESS / FAIL */
    @Column(nullable = false)
    private String status;

    @Column
    private String message;
}
