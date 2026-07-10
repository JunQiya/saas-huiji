package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 租户 */
@Entity
@Table(name = "tenant")
@Getter
@Setter
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "brand_color")
    private String brandColor;

    /** ACTIVE / DISABLED */
    @Column
    private String status = "ACTIVE";

    @Column(name = "expire_at")
    private java.time.LocalDateTime expireAt;
}
