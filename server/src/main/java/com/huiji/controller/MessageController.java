package com.huiji.controller;

import com.huiji.common.PageData;
import com.huiji.common.Result;
import com.huiji.dto.MessageDto;
import com.huiji.security.PreAllowed;
import com.huiji.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 消息中心接口(后台管理) */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@PreAllowed({"TENANT_ADMIN", "STORE_MANAGER"})
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    @PreAllowed({"TENANT_ADMIN", "STORE_MANAGER", "STAFF", "CASHIER"})
    public Result<PageData<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(messageService.list(status, channel, parse(start, false), parse(end, true), page, size));
    }

    @GetMapping("/{id}")
    @PreAllowed({"TENANT_ADMIN", "STORE_MANAGER", "STAFF", "CASHIER"})
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(messageService.detail(id));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@Valid @RequestBody MessageDto.CreateRequest req) {
        return Result.success(messageService.create(req));
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        messageService.cancel(id);
        return Result.success();
    }

    @PostMapping("/{id}/retry")
    public Result<Map<String, Object>> retry(@PathVariable Long id) {
        return Result.success(messageService.retry(id));
    }

    @GetMapping("/stats")
    @PreAllowed({"TENANT_ADMIN", "STORE_MANAGER", "STAFF", "CASHIER"})
    public Result<Map<String, Object>> stats() {
        return Result.success(messageService.stats());
    }

    private LocalDateTime parse(String s, boolean isEnd) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.length() == 10) {
                return isEnd ? LocalDateTime.parse(s + "T23:59:59") : LocalDateTime.parse(s + "T00:00:00");
            }
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
