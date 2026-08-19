package com.huiji.controller;

import com.huiji.common.Result;
import com.huiji.dto.CampaignDto;
import com.huiji.security.PreAllowed;
import com.huiji.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 营销活动接口 (写操作仅超管与店长) */
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@PreAllowed({"TENANT_ADMIN", "STORE_MANAGER"})
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping
    @PreAllowed({"TENANT_ADMIN", "STORE_MANAGER", "STAFF", "CASHIER"})
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) String status) {
        return Result.success(campaignService.list(status));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@Valid @RequestBody CampaignDto.CampaignRequest req) {
        return Result.success(campaignService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody CampaignDto.CampaignRequest req) {
        return Result.success(campaignService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        campaignService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/toggle")
    public Result<Map<String, Object>> toggle(@PathVariable Long id, @RequestBody CampaignDto.ToggleRequest req) {
        return Result.success(campaignService.toggle(id, req.getEnabled()));
    }

    @PostMapping("/{id}/preview")
    public Result<Map<String, Object>> preview(@PathVariable Long id) {
        return Result.success(campaignService.preview(id));
    }

    @GetMapping("/{id}/stats")
    @PreAllowed({"TENANT_ADMIN", "STORE_MANAGER", "STAFF", "CASHIER"})
    public Result<Map<String, Object>> stats(@PathVariable Long id) {
        return Result.success(campaignService.stats(id));
    }
}
