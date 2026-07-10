package com.huiji.service;

import com.huiji.entity.AuditLog;
import com.huiji.repository.AuditLogRepository;
import com.huiji.security.LoginUser;
import com.huiji.security.LoginUserHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 审计日志记录助手: 记录后台操作行为。 */
@Component
@RequiredArgsConstructor
public class AuditHelper {

    private final AuditLogRepository auditLogRepository;

    public void record(String action, String target, String detail) {
        AuditLog log = new AuditLog();
        LoginUser user = LoginUserHolder.getOrNull();
        if (user != null) {
            log.setTenantId(user.getTenantId());
            log.setOperatorId(user.getUserId());
            log.setOperatorName(user.getUsername());
        }
        log.setAction(action);
        log.setTarget(target);
        log.setDetail(detail);
        log.setIp(currentIp());
        auditLogRepository.save(log);
    }

    public void record(String action, String target) {
        record(action, target, null);
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String ip = req.getHeader("X-Forwarded-For");
                if (ip != null && !ip.isBlank()) {
                    return ip.split(",")[0].trim();
                }
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
