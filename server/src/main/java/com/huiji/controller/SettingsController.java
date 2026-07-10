package com.huiji.controller;

import com.huiji.common.Result;
import com.huiji.dto.SettingsDto;
import com.huiji.entity.Store;
import com.huiji.entity.TenantSetting;
import com.huiji.repository.StoreRepository;
import com.huiji.repository.TenantSettingRepository;
import com.huiji.security.LoginUser;
import com.huiji.security.LoginUserHolder;
import com.huiji.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 租户设置: 基础/品牌/计费/多店 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final TenantSettingRepository tenantSettingRepository;
    private final StoreRepository storeRepository;

    @GetMapping
    public Result<Map<String, Object>> get() {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(toMap(settingsService.getSettings(tenantId)));
    }

    @PutMapping
    public Result<Map<String, Object>> update(@Valid @RequestBody SettingsDto.SettingsRequest req) {
        Long tenantId = LoginUserHolder.currentTenantId();
        settingsService.update(tenantId, req);
        return Result.success(toMap(settingsService.getSettings(tenantId)));
    }

    // ============ 新增: 计费版与多店切换 ============

    /** 计费版信息: plan / expiresAt / startedAt / smsBalance / 各维度上限 */
    @GetMapping("/plan")
    public Result<Map<String, Object>> plan() {
        Long tenantId = LoginUserHolder.currentTenantId();
        TenantSetting ts = tenantSettingRepository.findByTenantId(tenantId).orElse(null);
        String plan = "FREE";
        int smsBalance = 0;
        Map<String, Object> limits = planLimits();
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("plan", plan);
        vo.put("smsBalance", smsBalance);
        vo.put("startedAt", ts == null ? null : ts.getCreatedAt());
        vo.put("expiresAt", null);
        vo.put("limits", limits);
        return Result.success(vo);
    }

    /** 模拟升级: 入参 {plan, months} */
    @PostMapping("/plan/upgrade")
    public Result<Map<String, Object>> upgrade(@RequestBody Map<String, Object> body) {
        if (body == null) {
            return plan();
        }
        Object planObj = body.get("plan");
        Object monthsObj = body.get("months");
        String plan = planObj == null ? "BASIC" : String.valueOf(planObj).toUpperCase();
        int months = 1;
        if (monthsObj instanceof Number) months = ((Number) monthsObj).intValue();
        if (months <= 0 || months > 36) months = 1;
        Long tenantId = LoginUserHolder.currentTenantId();
        TenantSetting ts = tenantSettingRepository.findByTenantId(tenantId).orElseGet(() -> {
            TenantSetting s = new TenantSetting();
            s.setTenantId(tenantId);
            return s;
        });
        LocalDateTime now = LocalDateTime.now();
        ts.setCreatedAt(now);
        ts.setUpdatedAt(now);
        tenantSettingRepository.save(ts);
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("plan", plan);
        vo.put("smsBalance", plan.equals("FLAGSHIP") ? 5000 : plan.equals("GROWTH") ? 1000 : 200);
        vo.put("startedAt", now);
        vo.put("expiresAt", now.plusMonths(months));
        vo.put("months", months);
        return Result.success(vo);
    }

    /** 当前门店 */
    @GetMapping("/store/current")
    public Result<Map<String, Object>> currentStore() {
        Long tenantId = LoginUserHolder.currentTenantId();
        LoginUser lu = LoginUserHolder.get();
        Long storeId = lu.getStoreId();
        if (storeId == null) {
            // 默认取第一个门店
            List<Store> all = storeRepository.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId);
            if (!all.isEmpty()) {
                storeId = all.get(0).getId();
            }
        }
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("storeId", storeId);
        if (storeId != null) {
            storeRepository.findById(storeId).ifPresent(s -> {
                if (Boolean.TRUE.equals(s.getDeleted())) return;
                if (!tenantId.equals(s.getTenantId())) return;
                vo.put("id", s.getId());
                vo.put("name", s.getName());
                vo.put("address", s.getAddress());
                vo.put("phone", s.getPhone());
            });
        }
        return Result.success(vo);
    }

    /** 切换门店: 仅把 storeId 写回 LoginUserHolder, 进程级别生效 */
    @PostMapping("/store/switch")
    public Result<Map<String, Object>> switchStore(@RequestBody Map<String, Object> body) {
        if (body == null || body.get("storeId") == null) {
            throw new IllegalArgumentException("storeId 不能为空");
        }
        long storeId = Long.parseLong(body.get("storeId").toString());
        Long tenantId = LoginUserHolder.currentTenantId();
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在"));
        if (Boolean.TRUE.equals(store.getDeleted()) || !tenantId.equals(store.getTenantId())) {
            throw new IllegalArgumentException("门店不存在");
        }
        LoginUser old = LoginUserHolder.get();
        LoginUser fresh = LoginUser.builder()
                .userId(old.getUserId())
                .tenantId(old.getTenantId())
                .username(old.getUsername())
                                .role(old.getRole())
                .storeId(store.getId())
                .build();
        LoginUserHolder.set(fresh);
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("storeId", store.getId());
        vo.put("name", store.getName());
        return Result.success(vo);
    }

    /** 当前计费版下各维度上限(FREE 基础配额) */
    private Map<String, Object> planLimits() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("members", 500);
        m.put("stores", 3);
        m.put("products", 30);
        m.put("employees", 5);
        return m;
    }

    /** 把 SettingsDto.SettingsRequest 转换为通用 Map 响应 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object src) {
        if (src == null) return new LinkedHashMap<>();
        if (src instanceof Map) return (Map<String, Object>) src;
        Map<String, Object> m = new LinkedHashMap<>();
        if (src instanceof SettingsDto.SettingsRequest) {
            SettingsDto.SettingsRequest r = (SettingsDto.SettingsRequest) src;
            m.put("tenantName", r.getTenantName());
            m.put("brandColor", r.getBrandColor());
            m.put("smsSign", r.getSmsSign());
            m.put("levelRules", r.getLevelRules());
            m.put("rechargeRules", r.getRechargeRules());
        }
        return m;
    }
}