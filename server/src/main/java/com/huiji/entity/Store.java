package com.huiji.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 门店 */
@Entity
@Table(name = "store")
@Getter
@Setter
public class Store extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column
    private String address;

    @Column
    private String phone;

    @Column(name = "business_hours")
    private String businessHours;

    /** ACTIVE / DISABLED */
    @Column
    private String status = "ACTIVE";
}
