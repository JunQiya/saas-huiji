package com.huiji.controller;

import com.huiji.common.Result;
import com.huiji.dto.SettingsDto;
import com.huiji.entity.Store;
import com.huiji.entity.TenantSetting;
import com.huiji.repository.StoreRepository;
import com.huiji.repository.TenantSettingRepository;
import com.huiji.security.JwtUtil;
import com.huiji.security.LoginUser;
import com.huiji.security.LoginUserHolder;
import com.huiji.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
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
    private final JwtUtil jwtUtil;

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

    /** 获取功能开关 */
    @GetMapping("/features")
    public Result<SettingsDto.FeatureFlags> features() {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(settingsService.getFeatureFlags(tenantId));
    }

    /** 更新功能开关 */
    @PutMapping("/features")
    public Result<SettingsDto.FeatureFlags> updateFeatures(@RequestBody SettingsDto.FeatureFlags flags) {
        Long tenantId = LoginUserHolder.currentTenantId();
        return Result.success(settingsService.updateFeatureFlags(tenantId, flags));
    }

    // ============ 新增: 计费版与多店切换 ============

    /** 计费版信息: plan / expiresAt / startedAt / smsBalance / 各维度上限 */
    @GetMapping("/plan")
    public Result<Map<String, Object>> plan() {
        Long tenantId = LoginUserHolder.currentTenantId();
        TenantSetting ts = tenantSettingRepository.findByTenantId(tenantId).orElse(null);
        String plan = ts == null || ts.getPlan() == null ? "FREE" : ts.getPlan();
        int smsBalance = ts == null || ts.getSmsBalance() == null ? 0 : ts.getSmsBalance();
        Map<String, Object> limits = planLimits(plan);
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("plan", plan);
        vo.put("smsBalance", smsBalance);
        vo.put("startedAt", ts == null ? null : ts.getCreatedAt());
        vo.put("expiresAt", ts == null ? null : ts.getPlanExpiresAt());
        vo.put("limits", limits);
        return Result.success(vo);
    }

    /** 升级套餐: 入参 {plan, months}，持久化到租户设置 */
    @PostMapping("/plan/upgrade")
    @Transactional
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
        LocalDateTime base = ts.getPlanExpiresAt() != null && ts.getPlanExpiresAt().isAfter(now)
                ? ts.getPlanExpiresAt() : now;
        ts.setPlan(plan);
        ts.setPlanExpiresAt(base.plusMonths(months));
        ts.setSmsBalance(plan.equals("FLAGSHIP") ? 5000 : plan.equals("GROWTH") ? 1000 : plan.equals("BASIC") ? 200 : 0);
        if (ts.getCreatedAt() == null) ts.setCreatedAt(now);
        ts.setUpdatedAt(now);
        tenantSettingRepository.save(ts);
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("plan", plan);
        vo.put("smsBalance", ts.getSmsBalance());
        vo.put("startedAt", ts.getCreatedAt());
        vo.put("expiresAt", ts.getPlanExpiresAt());
        vo.put("months", months);
        vo.put("limits", planLimits(plan));
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

    /** 切换门店: 生成含新 storeId 的 token 返回前端 */
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
        String newToken = jwtUtil.generate(old.getUserId(), old.getTenantId(), old.getUsername(), old.getRole(), store.getId());
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("storeId", store.getId());
        vo.put("name", store.getName());
        vo.put("token", newToken);
        return Result.success(vo);
    }

    /** 各套餐的配额上限 */
    private Map<String, Object> planLimits(String plan) {
        Map<String, Object> m = new LinkedHashMap<>();
        switch (plan == null ? "FREE" : plan) {
            case "FLAGSHIP":
                m.put("members", "不限"); m.put("stores", "不限"); m.put("products", 1000); m.put("employees", "不限");
                break;
            case "GROWTH":
                m.put("members", 50000); m.put("stores", 30); m.put("products", 500); m.put("employees", 50);
                break;
            case "BASIC":
                m.put("members", 5000); m.put("stores", 10); m.put("products", 100); m.put("employees", 20);
                break;
            default:
                m.put("members", 500); m.put("stores", 3); m.put("products", 30); m.put("employees", 5);
        }
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