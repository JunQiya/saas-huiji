package com.huiji.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色校验注解, 标注在 Controller 方法上, 由 RoleAspect 解析。
 * 未标注 @PreAllowed 的方法默认要求登录即可访问。
 * 示例: @PreAllowed({"TENANT_ADMIN"})
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PreAllowed {

    /** 允许的角色, 满足其一即可 */
    String[] value();
}
