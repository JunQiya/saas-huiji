package com.huiji.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.dto.SettingsDto;
import com.huiji.entity.TenantSetting;
import com.huiji.repository.TenantSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户设置服务: 等级规则、储值规则、品牌信息。
 * levelRules / rechargeRules 以 JSON 字符串存储, 读写时序列化。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final TenantSettingRepository settingRepository;
    private final ObjectMapper objectMapper;

    /** 默认等级规则 */
    private static final String DEFAULT_LEVEL_RULES =
            "[{\"level\":1,\"name\":\"普通会员\",\"threshold\":0}," +
                    "{\"level\":2,\"name\":\"银卡会员\",\"threshold\":50000}," +
                    "{\"level\":3,\"name\":\"金卡会员\",\"threshold\":200000}," +
                    "{\"level\":4,\"name\":\"钻石会员\",\"threshold\":500000}]";

    /** 默认储值赠送规则: 充 1000 送 100, 充 2000 送 300, 充 5000 送 1000 */
    private static final String DEFAULT_RECHARGE_RULES =
            "[{\"recharge\":100000,\"gift\":10000}," +
                    "{\"recharge\":200000,\"gift\":30000}," +
                    "{\"recharge\":500000,\"gift\":100000}]";

    /** 默认功能开关（全部开启，短信默认关闭） */
    private static final String DEFAULT_FEATURE_FLAGS =
            "{\"pointsEnabled\":true,\"rechargeEnabled\":true,\"couponsEnabled\":true," +
                    "\"campaignsEnabled\":true,\"referralEnabled\":true,\"birthdayMarketingEnabled\":true," +
                    "\"smsEnabled\":false,\"selfRegisterEnabled\":true,\"autoUpgradeEnabled\":true," +
                    "\"receiptPrintEnabled\":true}";

    public TenantSetting getOrInit(Long tenantId, String tenantName) {
        return settingRepository.findByTenantId(tenantId).orElseGet(() -> {
            TenantSetting s = new TenantSetting();
            s.setTenantId(tenantId);
            s.setTenantName(tenantName);
            s.setBrandColor("#4f46e5");
            s.setSmsSign("星河会记");
            s.setLevelRules(DEFAULT_LEVEL_RULES);
            s.setRechargeRules(DEFAULT_RECHARGE_RULES);
            s.setFeatureFlags(DEFAULT_FEATURE_FLAGS);
            return settingRepository.save(s);
        });
    }

    public SettingsDto.SettingsRequest getSettings(Long tenantId) {
        TenantSetting s = getOrInit(tenantId, null);
        SettingsDto.SettingsRequest dto = new SettingsDto.SettingsRequest();
        dto.setTenantName(s.getTenantName());
        dto.setBrandColor(s.getBrandColor());
        dto.setSmsSign(s.getSmsSign());
        dto.setLevelRules(parseRules(s.getLevelRules(), SettingsDto.LevelRule.class));
        dto.setRechargeRules(parseRules(s.getRechargeRules(), SettingsDto.RechargeRule.class));
        return dto;
    }

    /** 各套餐的配额上限 */
    public Map<String, Object> planLimits(String plan) {
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

    /** 升级套餐: 按 plan/months 延长有效期并重置短信余额 */
    @Transactional
    public Map<String, Object> upgrade(Long tenantId, String plan, int months) {
        TenantSetting ts = settingRepository.findByTenantId(tenantId).orElseGet(() -> {
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
        settingRepository.save(ts);
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("plan", plan);
        vo.put("smsBalance", ts.getSmsBalance());
        vo.put("startedAt", ts.getCreatedAt());
        vo.put("expiresAt", ts.getPlanExpiresAt());
        vo.put("months", months);
        vo.put("limits", planLimits(plan));
        return vo;
    }

    @Transactional
    public void update(Long tenantId, SettingsDto.SettingsRequest req) {
        TenantSetting s = getOrInit(tenantId, null);
        if (req.getTenantName() != null) s.setTenantName(req.getTenantName());
        if (req.getBrandColor() != null) s.setBrandColor(req.getBrandColor());
        if (req.getSmsSign() != null) s.setSmsSign(req.getSmsSign());
        if (req.getLevelRules() != null) {
            s.setLevelRules(toJson(req.getLevelRules()));
        }
        if (req.getRechargeRules() != null) {
            s.setRechargeRules(toJson(req.getRechargeRules()));
        }
        settingRepository.save(s);
    }

    /** 等级规则列表(按 level 升序) */
    public List<SettingsDto.LevelRule> levelRules(Long tenantId) {
        TenantSetting s = getOrInit(tenantId, null);
        List<SettingsDto.LevelRule> rules = parseRules(s.getLevelRules(), SettingsDto.LevelRule.class);
        rules.sort(Comparator.comparingInt(SettingsDto.LevelRule::getLevel));
        return rules;
    }

    /** 储值赠送规则列表 */
    public List<SettingsDto.RechargeRule> rechargeRules(Long tenantId) {
        TenantSetting s = getOrInit(tenantId, null);
        return parseRules(s.getRechargeRules(), SettingsDto.RechargeRule.class);
    }

    /** 根据累计消费金额计算应处等级(取满足阈值的最高等级) */
    public SettingsDto.LevelRule resolveLevel(Long tenantId, Long totalAmount) {
        List<SettingsDto.LevelRule> rules = levelRules(tenantId);
        SettingsDto.LevelRule matched = null;
        for (SettingsDto.LevelRule r : rules) {
            if (totalAmount == null) totalAmount = 0L;
            if (totalAmount >= r.getThreshold()) {
                if (matched == null || r.getLevel() > matched.getLevel()) {
                    matched = r;
                }
            }
        }
        return matched;
    }

    /** 等级名称 */
    public String levelName(Long tenantId, Integer level) {
        if (level == null) return "普通会员";
        return levelRules(tenantId).stream()
                .filter(r -> r.getLevel().equals(level))
                .map(SettingsDto.LevelRule::getName)
                .findFirst().orElse("普通会员");
    }

    /** 根据充值金额匹配赠送金额(取满足充值额的最大赠送) */
    public Long matchGift(Long tenantId, Long rechargeAmount) {
        List<SettingsDto.RechargeRule> rules = rechargeRules(tenantId);
        Long gift = 0L;
        for (SettingsDto.RechargeRule r : rules) {
            if (rechargeAmount >= r.getRecharge()) {
                if (r.getGift() != null && r.getGift() > gift) {
                    gift = r.getGift();
                }
            }
        }
        return gift;
    }

    /** 获取功能开关 */
    public SettingsDto.FeatureFlags getFeatureFlags(Long tenantId) {
        TenantSetting s = getOrInit(tenantId, null);
        String json = s.getFeatureFlags();
        if (json == null || json.isBlank()) {
            return parseFlags(DEFAULT_FEATURE_FLAGS);
        }
        return parseFlags(json);
    }

    /** 更新功能开关 */
    @Transactional
    public SettingsDto.FeatureFlags updateFeatureFlags(Long tenantId, SettingsDto.FeatureFlags flags) {
        TenantSetting s = getOrInit(tenantId, null);
        s.setFeatureFlags(toJson(flags));
        settingRepository.save(s);
        return flags;
    }

    private SettingsDto.FeatureFlags parseFlags(String json) {
        if (json == null || json.isBlank()) {
            SettingsDto.FeatureFlags f = new SettingsDto.FeatureFlags();
            return f;
        }
        try {
            return objectMapper.readValue(json, SettingsDto.FeatureFlags.class);
        } catch (Exception e) {
            log.warn("解析功能开关 JSON 失败: {}", e.getMessage());
            return new SettingsDto.FeatureFlags();
        }
    }

    private <T> List<T> parseRules(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            log.warn("解析规则 JSON 失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVER_ERROR, "规则序列化失败");
        }
    }
}
