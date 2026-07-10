package com.huiji.security;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 计费版限额拦截(占位实现): 现在由各 Service 在 create 内部自行调用 checkQuota(...),
 * 此处仅作为 no-op 保留, 以兼容历史 @PlanLimitCheck 注解, 未来若改为统一拦截可在此扩展。
 */
@Slf4j
@Aspect
@Component
public class PlanLimitAspect {

    @Around("execution(* com.huiji.service.*.create(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        // 让 Service 自行 checkQuota, 此处不拦截
        return pjp.proceed();
    }
}
