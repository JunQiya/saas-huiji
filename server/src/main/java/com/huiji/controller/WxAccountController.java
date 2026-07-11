package com.huiji.controller;

import com.huiji.common.Result;
import com.huiji.entity.WxAccount;
import com.huiji.repository.WxAccountRepository;
import com.huiji.security.LoginUserHolder;
import com.huiji.service.WxMpConfigService;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 微信公众号配置管理 */
@RestController
@RequestMapping("/api/wx/account")
@RequiredArgsConstructor
public class WxAccountController {

    private final WxMpConfigService wxMpConfigService;
    private final WxAccountRepository wxAccountRepository;

    /** 获取当前租户的微信配置 */
    @GetMapping
    public Result<Map<String, Object>> get() {
        Long tenantId = LoginUserHolder.currentTenantId();
        WxAccount account = wxMpConfigService.getAccount(tenantId);
        return Result.success(toView(account));
    }

    /** 保存/更新配置 */
    @PutMapping
    public Result<Map<String, Object>> save(@RequestBody Map<String, Object> body) {
        Long tenantId = LoginUserHolder.currentTenantId();
        WxAccount account = wxAccountRepository.findByTenantId(tenantId).orElseGet(() -> {
            WxAccount a = new WxAccount();
            a.setTenantId(tenantId);
            return a;
        });
        if (body.containsKey("agentId")) account.setAgentId(toLong(body.get("agentId")));
        if (body.containsKey("appId")) account.setAppId(str(body.get("appId")));
        if (body.containsKey("appSecret")) account.setAppSecret(str(body.get("appSecret")));
        if (body.containsKey("mchId")) account.setMchId(str(body.get("mchId")));
        if (body.containsKey("mchKey")) account.setMchKey(str(body.get("mchKey")));
        if (body.containsKey("apiV3Key")) account.setApiV3Key(str(body.get("apiV3Key")));
        if (body.containsKey("certPath")) account.setCertPath(str(body.get("certPath")));
        if (body.containsKey("templateIds")) account.setTemplateIds(str(body.get("templateIds")));
        if (body.containsKey("domain")) account.setDomain(str(body.get("domain")));
        if (body.containsKey("status")) account.setStatus(str(body.get("status")));
        wxAccountRepository.save(account);
        wxMpConfigService.refresh(tenantId);
        return Result.success(toView(account));
    }

    /** 测试连通性(尝试获取 access_token) */
    @GetMapping("/test")
    public Result<Map<String, Object>> test() {
        Long tenantId = LoginUserHolder.currentTenantId();
        WxMpService mpService = wxMpConfigService.getMpService(tenantId);
        if (mpService == null) {
            return Result.fail("NOT_CONFIGURED", "未配置微信公众号");
        }
        try {
            String accessToken = mpService.getAccessToken();
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("ok", true);
            vo.put("appId", mpService.getWxMpConfigStorage().getAppId());
            vo.put("accessToken", accessToken);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.fail("TEST_FAIL", "连通性测试失败: " + e.getMessage());
        }
    }

    /** 转视图, 敏感字段打掩码 */
    private Map<String, Object> toView(WxAccount account) {
        Map<String, Object> vo = new LinkedHashMap<>();
        if (account == null) return vo;
        vo.put("id", account.getId());
        vo.put("tenantId", account.getTenantId());
        vo.put("agentId", account.getAgentId());
        vo.put("appId", account.getAppId());
        vo.put("appSecret", mask(account.getAppSecret()));
        vo.put("mchId", account.getMchId());
        vo.put("mchKey", mask(account.getMchKey()));
        vo.put("apiV3Key", mask(account.getApiV3Key()));
        vo.put("certPath", account.getCertPath());
        vo.put("templateIds", account.getTemplateIds());
        vo.put("domain", account.getDomain());
        vo.put("status", account.getStatus());
        return vo;
    }

    private static String mask(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.length() <= 4) return "****";
        return "****" + s.substring(s.length() - 4);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
