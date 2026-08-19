package com.huiji.entity;

import com.huiji.entity.converter.LongListConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** 员工/用户(后台登录账号) */
@Entity
@Table(name = "app_user")
@Getter
@Setter
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String username;

    /** 密码哈希, 严禁序列化到任何接口响应 */
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column
    private String phone;

    /** TENANT_ADMIN / STORE_MANAGER / STAFF / CASHIER */
    @Column(nullable = false)
    private String role;

    @Convert(converter = LongListConverter.class)
    @Column(name = "store_ids")
    private List<Long> storeIds = new ArrayList<>();

    /** ACTIVE / DISABLED */
    @Column
    private String status = "ACTIVE";
}
