package com.huiji.security;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @PreAllowed 角色校验切面: 方法或类上标注的角色与当前登录用户角色匹配。
 */
@Aspect
@Component
public class RoleAspect {

    @Before("@within(com.huiji.security.PreAllowed) || @annotation(com.huiji.security.PreAllowed)")
    public void check(JoinPoint jp) {
        MethodSignature sig = (MethodSignature) jp.getSignature();
        Method method = sig.getMethod();
        PreAllowed pa = method.getAnnotation(PreAllowed.class);
        if (pa == null) {
            pa = method.getDeclaringClass().getAnnotation(PreAllowed.class);
        }
        if (pa == null) {
            return;
        }
        LoginUser user = LoginUserHolder.getOrNull();
        if (user == null) {
            throw new BizException(ErrorCode.SESSION_EXPIRED);
        }
        String role = user.getRole();
        boolean ok = Arrays.asList(pa.value()).contains(role);
        if (!ok) {
            throw new BizException(ErrorCode.FORBIDDEN, "当前角色无权操作");
        }
    }
}
