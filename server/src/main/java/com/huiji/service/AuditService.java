package com.huiji.service;

import com.huiji.common.PageData;
import com.huiji.entity.AuditLog;
import com.huiji.entity.LoginLog;
import com.huiji.repository.AuditLogRepository;
import com.huiji.repository.LoginLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 审计服务: 操作日志、登录日志。 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final LoginLogRepository loginLogRepository;

    public PageData<Map<String, Object>> logs(String operator, String action,
                                              LocalDateTime start, LocalDateTime end,
                                              int page, int size) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size <= 0 ? 20 : size);
        Page<AuditLog> p = auditLogRepository.search(tenantId, operator, action, start, end, pageable);
        List<Map<String, Object>> list = p.getContent().stream().map(this::logVO).toList();
        return PageData.of(list, p.getTotalElements(), page, size);
    }

    public PageData<Map<String, Object>> logins(int page, int size) {
        Long tenantId = com.huiji.security.LoginUserHolder.currentTenantId();
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size <= 0 ? 20 : size);
        Page<LoginLog> p = loginLogRepository.findByTenantIdOrderByIdDesc(tenantId, pageable);
        List<Map<String, Object>> list = p.getContent().stream().map(this::loginVO).toList();
        return PageData.of(list, p.getTotalElements(), page, size);
    }

    private Map<String, Object> logVO(AuditLog a) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", a.getId());
        vo.put("operatorId", a.getOperatorId());
        vo.put("operatorName", a.getOperatorName());
        vo.put("action", a.getAction());
        vo.put("target", a.getTarget());
        vo.put("detail", a.getDetail());
        vo.put("ip", a.getIp());
        vo.put("createdAt", a.getCreatedAt());
        return vo;
    }

    private Map<String, Object> loginVO(LoginLog l) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", l.getId());
        vo.put("userId", l.getUserId());
        vo.put("username", l.getUsername());
        vo.put("ip", l.getIp());
        vo.put("status", l.getStatus());
        vo.put("message", l.getMessage());
        vo.put("createdAt", l.getCreatedAt());
        return vo;
    }
}
