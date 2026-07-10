package com.huiji.controller;

import com.huiji.common.PageData;
import com.huiji.common.Result;
import com.huiji.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/** 审计接口 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    public Result<PageData<Map<String, Object>>> logs(
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LocalDateTime startTime = parse(start);
        LocalDateTime endTime = parse(end);
        return Result.success(auditService.logs(operator, action, startTime, endTime, page, size));
    }

    @GetMapping("/logins")
    public Result<PageData<Map<String, Object>>> logins(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(auditService.logins(page, size));
    }

    private LocalDateTime parse(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s.replace(' ', 'T'));
        } catch (Exception e) {
            return null;
        }
    }
}
