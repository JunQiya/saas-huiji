package com.huiji.controller;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import com.huiji.common.Result;
import com.huiji.entity.Member;
import com.huiji.entity.WxAccount;
import com.huiji.repository.MemberRepository;
import com.huiji.security.LoginUserHolder;
import com.huiji.security.MemberTokenUtil;
import com.huiji.service.WxMpConfigService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 微信 H5 交互: OAuth 授权、JS-SDK 签名、模板消息。
 * 路径前缀 /api/wx, 其中 oauth/callback 公开, jssdk 需要 memberToken, template/send 需要 adminToken。
 */
@Slf4j
@RestController
@RequestMapping("/api/wx")
@RequiredArgsConstructor
public class WeChatController {

    private final WxMpConfigService wxMpConfigService;
    private final MemberRepository memberRepository;
    private final MemberTokenUtil memberTokenUtil;

    @Value("${huiji.h5-domain:}")
    private String h5Domain;

    /** 1. OAuth 授权入口: 302 重定向到微信授权页 */
    @GetMapping("/oauth/{tenantId}")
    public void oauth(@PathVariable Long tenantId,
                      @RequestParam(required = false) String redirect,
                      @RequestParam(required = false) String state,
                      HttpServletResponse response) throws IOException {
        WxAccount account = wxMpConfigService.getAccount(tenantId);
        String domain = resolveDomain(account);
        // 微信回调地址, 默认用本服务 /api/wx/callback
        String redirectUri = (redirect != null && !redirect.isBlank())
                ? redirect
                : domain + "/api/wx/callback";
        // state 格式: tenantId:{extra}, 回调时解析出 tenantId
        String fullState = tenantId + ":" + (state == null ? "" : state);
        String authorizeUrl = wxMpConfigService.getAuthorizeUrl(tenantId, redirectUri, "snsapi_userinfo", fullState);
        if (authorizeUrl == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "未配置微信公众号");
            return;
        }
        response.sendRedirect(authorizeUrl);
    }

    /** 2. OAuth 回调: 换 token、取用户信息、查/建会员、生成 memberToken, 重定向回 H5 */
    @GetMapping("/callback")
    public void callback(@RequestParam String code,
                         @RequestParam String state,
                         HttpServletResponse response) throws IOException {
        Long tenantId = null;
        WxAccount account = null;
        try {
            // 从 state 解析 tenantId
            String[] parts = state.split(":", 2);
            tenantId = Long.parseLong(parts[0]);
            String extra = parts.length > 1 ? parts[1] : "";

            account = wxMpConfigService.getAccount(tenantId);
            if (account == null) {
                throw new RuntimeException("租户未配置微信公众号");
            }

            // 换 access_token, 取用户信息
            WxOAuth2AccessToken token = wxMpConfigService.oauth2getAccessToken(tenantId, code);
            WxOAuth2UserInfo userInfo = wxMpConfigService.oauth2getUserInfo(tenantId, token);
            if (userInfo == null || userInfo.getOpenid() == null) {
                throw new RuntimeException("获取微信用户信息失败");
            }

            // 查/建会员
            final String openid = userInfo.getOpenid();
            final Long tid = tenantId;
            Member member = memberRepository.findByWxOpenidAndTenantIdAndDeletedFalse(openid, tenantId)
                    .orElseGet(() -> {
                        Member m = new Member();
                        m.setTenantId(tid);
                        m.setWxOpenid(openid);
                        m.setPhone("");
                        m.setName(userInfo.getNickname() != null ? userInfo.getNickname() : "微信用户");
                        m.setGender(mapGender(userInfo.getSex()));
                        m.setWxHeadImgUrl(userInfo.getHeadImgUrl());
                        return memberRepository.save(m);
                    });

            // 更新昵称/头像/登录时间
            if (userInfo.getNickname() != null && !userInfo.getNickname().isBlank()) {
                member.setName(userInfo.getNickname());
            }
            if (userInfo.getHeadImgUrl() != null && !userInfo.getHeadImgUrl().isBlank()) {
                member.setWxHeadImgUrl(userInfo.getHeadImgUrl());
            }
            member.setLastLoginAt(LocalDateTime.now());
            memberRepository.save(member);

            // 生成 memberToken
            String memberToken = memberTokenUtil.generate(member.getId(), tenantId);

            // 重定向回 H5 前端
            String domain = resolveDomain(account);
            String redirectUrl = domain + "/#/wx-login?token=" + URLEncoder.encode(memberToken, StandardCharsets.UTF_8)
                    + "&state=" + URLEncoder.encode(extra, StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("微信授权回调失败 tenantId={}", tenantId, e);
            String domain = resolveDomain(account);
            response.sendRedirect(domain + "/#/login?error=wx_auth_failed");
        }
    }

    /** 3. 获取 JS-SDK 签名配置(需要 memberToken) */
    @GetMapping("/jssdk")
    public Result<Map<String, String>> jssdk(HttpServletRequest req, @RequestParam String url) {
        long[] ctx = currentMember(req);
        Map<String, String> signature = wxMpConfigService.createJsapiSignature(ctx[1], url);
        if (signature == null) {
            return Result.success(null);
        }
        return Result.success(signature);
    }

    /** 4. 发送模板消息(需要 adminToken) */
    @PostMapping("/template/send")
    public Result<Void> sendTemplate(@RequestBody Map<String, Object> body) {
        Long tenantId = LoginUserHolder.currentTenantId();
        String openid = str(body.get("openid"));
        String templateKey = str(body.get("templateKey"));
        String url = str(body.get("url"));
        if (openid == null || openid.isBlank()) {
            throw new BizException(ErrorCode.VALIDATION, "openid 不能为空");
        }
        if (templateKey == null || templateKey.isBlank()) {
            throw new BizException(ErrorCode.VALIDATION, "templateKey 不能为空");
        }
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) body.get("data");
        wxMpConfigService.sendTemplateMessage(tenantId, openid, templateKey, data, url);
        return Result.success();
    }

    // ---- 工具方法 ----

    /** 解析 H5 域名: 优先取 WxAccount.domain, 回退到配置 huiji.h5-domain */
    private String resolveDomain(WxAccount account) {
        if (account != null && account.getDomain() != null && !account.getDomain().isBlank()) {
            return account.getDomain().replaceAll("/+$", "");
        }
        if (h5Domain != null && !h5Domain.isBlank()) {
            return h5Domain.replaceAll("/+$", "");
        }
        return "";
    }

    /** 微信性别映射 */
    private String mapGender(Integer sex) {
        if (sex == null) return "UNKNOWN";
        return switch (sex) {
            case 1 -> "MALE";
            case 2 -> "FEMALE";
            default -> "UNKNOWN";
        };
    }

    /** 解析 memberToken, 返回 [memberId, tenantId] */
    private long[] currentMember(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.SESSION_EXPIRED, "请先登录");
        }
        String token = header.substring(7);
        try {
            Claims claims = memberTokenUtil.parse(token);
            if (!"MEMBER".equals(claims.get("type", String.class))) {
                throw new BizException(ErrorCode.SESSION_EXPIRED, "登录态无效");
            }
            Long memberId = claims.get("memberId", Long.class);
            Long tenantId = claims.get("tenantId", Long.class);
            if (memberId == null) {
                throw new BizException(ErrorCode.SESSION_EXPIRED, "登录态无效");
            }
            return new long[]{memberId, tenantId == null ? 1L : tenantId};
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SESSION_EXPIRED, "登录已过期");
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
