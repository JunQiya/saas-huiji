package com.huiji.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiji.entity.Agent;
import com.huiji.entity.WxAccount;
import com.huiji.repository.AgentRepository;
import com.huiji.repository.WxAccountRepository;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxJsapiSignature;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多租户微信配置管理: 根据租户 ID 获取对应的 WxMpService / WxPayService 实例。
 * 优先使用租户自配公众号, 未配置则回退到挂靠代理商的公众号。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxMpConfigService {

    private final WxAccountRepository wxAccountRepository;
    private final AgentRepository agentRepository;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<Long, WxMpService> mpServiceCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> mpAppIdCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, WxPayService> payServiceCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> payMchIdCache = new ConcurrentHashMap<>();

    /** 获取租户启用的微信账号配置 */
    public WxAccount getAccount(Long tenantId) {
        return wxAccountRepository.findByTenantIdAndStatus(tenantId, "ENABLED").orElse(null);
    }

    private Agent getAgent(Long agentId) {
        if (agentId == null) return null;
        return agentRepository.findByIdAndStatus(agentId, "ENABLED").orElse(null);
    }

    /**
     * 获取公众号服务实例, appId 变更时自动重建。
     * 返回 null 表示未配置微信。
     */
    public WxMpService getMpService(Long tenantId) {
        WxAccount account = getAccount(tenantId);
        if (account == null) return null;

        String appId = account.getAppId();
        String appSecret = account.getAppSecret();
        if (isBlank(appId) && account.getAgentId() != null) {
            Agent agent = getAgent(account.getAgentId());
            if (agent != null) {
                appId = agent.getAppId();
                appSecret = agent.getAppSecret();
            }
        }
        if (isBlank(appId)) return null;

        String cachedAppId = mpAppIdCache.get(tenantId);
        WxMpService cached = mpServiceCache.get(tenantId);
        if (cached != null && appId.equals(cachedAppId)) {
            return cached;
        }

        WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
        config.setAppId(appId);
        config.setSecret(appSecret);
        WxMpServiceImpl mpService = new WxMpServiceImpl();
        mpService.setWxMpConfigStorage(config);

        mpServiceCache.put(tenantId, mpService);
        mpAppIdCache.put(tenantId, appId);
        return mpService;
    }

    /**
     * 获取支付服务实例, mchId 变更时自动重建。
     * 返回 null 表示未配置微信支付。
     */
    public WxPayService getPayService(Long tenantId) {
        WxAccount account = getAccount(tenantId);
        if (account == null) return null;

        String appId = account.getAppId();
        String mchId = account.getMchId();
        String mchKey = account.getMchKey();
        String apiV3Key = account.getApiV3Key();
        if (isBlank(mchId) && account.getAgentId() != null) {
            Agent agent = getAgent(account.getAgentId());
            if (agent != null) {
                mchId = agent.getMchId();
                mchKey = agent.getMchKey();
                if (isBlank(appId)) appId = agent.getAppId();
            }
        }
        if (isBlank(mchId)) return null;

        String cachedMchId = payMchIdCache.get(tenantId);
        WxPayService cached = payServiceCache.get(tenantId);
        if (cached != null && mchId.equals(cachedMchId)) {
            return cached;
        }

        WxPayConfig payConfig = new WxPayConfig();
        payConfig.setAppId(appId);
        payConfig.setMchId(mchId);
        payConfig.setMchKey(mchKey);
        if (!isBlank(apiV3Key)) {
            payConfig.setApiV3Key(apiV3Key);
        }
        if (!isBlank(account.getCertPath())) {
            payConfig.setKeyPath(account.getCertPath());
        }
        WxPayServiceImpl payService = new WxPayServiceImpl();
        payService.setConfig(payConfig);

        payServiceCache.put(tenantId, payService);
        payMchIdCache.put(tenantId, mchId);
        return payService;
    }

    /** 刷新缓存(配置更新后调用) */
    public void refresh(Long tenantId) {
        mpServiceCache.remove(tenantId);
        mpAppIdCache.remove(tenantId);
        payServiceCache.remove(tenantId);
        payMchIdCache.remove(tenantId);
    }

    /** 获取 OAuth 网页授权链接 */
    public String getAuthorizeUrl(Long tenantId, String redirectUri, String scope, String state) {
        WxMpService mpService = getMpService(tenantId);
        if (mpService == null) return null;
        return mpService.getOAuth2Service().buildAuthorizationUrl(redirectUri, scope, state);
    }

    /** 用 code 换 access_token */
    public WxOAuth2AccessToken oauth2getAccessToken(Long tenantId, String code) {
        WxMpService mpService = getMpService(tenantId);
        if (mpService == null) return null;
        try {
            return mpService.getOAuth2Service().getAccessToken(code);
        } catch (Exception e) {
            throw new RuntimeException("微信授权失败: " + e.getMessage(), e);
        }
    }

    /** 获取用户信息(scope=snsapi_userinfo 时可调用) */
    public WxOAuth2UserInfo oauth2getUserInfo(Long tenantId, WxOAuth2AccessToken token) {
        WxMpService mpService = getMpService(tenantId);
        if (mpService == null) return null;
        try {
            return mpService.getOAuth2Service().getUserInfo(token, "zh_CN");
        } catch (Exception e) {
            throw new RuntimeException("获取微信用户信息失败: " + e.getMessage(), e);
        }
    }

    /** JS-SDK 签名 */
    public Map<String, String> createJsapiSignature(Long tenantId, String url) {
        WxMpService mpService = getMpService(tenantId);
        if (mpService == null) return null;
        try {
            WxJsapiSignature sig = mpService.createJsapiSignature(url);
            Map<String, String> result = new LinkedHashMap<>();
            result.put("appId", sig.getAppId());
            result.put("nonceStr", sig.getNonceStr());
            result.put("timestamp", String.valueOf(sig.getTimestamp()));
            result.put("signature", sig.getSignature());
            result.put("url", sig.getUrl());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("JS-SDK 签名失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送模板消息。
     * templateKey 映射到 WxAccount.templateIds JSON 中对应的模板 ID。
     */
    @SuppressWarnings("unchecked")
    public void sendTemplateMessage(Long tenantId, String openid, String templateKey,
                                    Map<String, String> data, String url) {
        WxMpService mpService = getMpService(tenantId);
        if (mpService == null) return;
        WxAccount account = getAccount(tenantId);
        if (account == null || isBlank(account.getTemplateIds())) return;

        Map<String, String> templateIds;
        try {
            templateIds = objectMapper.readValue(account.getTemplateIds(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("解析模板消息配置失败", e);
        }
        String templateId = templateIds.get(templateKey);
        if (isBlank(templateId)) return;

        WxMpTemplateMessage message = WxMpTemplateMessage.builder()
                .toUser(openid)
                .templateId(templateId)
                .url(url)
                .build();
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                message.addData(new WxMpTemplateData(entry.getKey(), entry.getValue()));
            }
        }
        try {
            mpService.getTemplateMsgService().sendTemplateMsg(message);
        } catch (Exception e) {
            throw new RuntimeException("发送模板消息失败: " + e.getMessage(), e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
