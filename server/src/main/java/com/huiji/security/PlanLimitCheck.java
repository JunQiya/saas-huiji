package com.huiji.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 Service.create(...) 方法参与计费版限额检查。
 *  - value: 配额键(members / stores / products / employees)
 *  - 拦截由 PlanLimitAspect 处理
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PlanLimitCheck {
    String value();
}
