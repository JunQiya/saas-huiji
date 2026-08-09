package com.huiji.security;

import com.huiji.common.BizException;
import com.huiji.common.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

import java.util.function.Function;

/**
 * 会员端上下文解析工具：统一从 memberToken 解析 memberId/tenantId，
 * 收敛各 H5 控制器中重复的 currentMember 实现。
 */
public final class MemberContext {

    public static final long DEFAULT_TENANT_ID = 1L;

    private MemberContext() {
    }

    /**
     * 从请求头解析 memberToken，返回 [memberId, tenantId]。
     * 未携带 token / 解析失败抛 SESSION_EXPIRED。
     */
    public static long[] require(HttpServletRequest req, MemberTokenUtil tokenUtil) {
        return require(req, tokenUtil, null);
    }

    /**
     * 从请求头解析 memberToken，返回 [memberId, tenantId]。
     *
     * @param tenantFallback token 中无 tenantId 时，可用 memberId 回查租户；传 null 则回退默认租户
     */
    public static long[] require(HttpServletRequest req, MemberTokenUtil tokenUtil,
                                 Function<Long, Long> tenantFallback) {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.SESSION_EXPIRED, "请先登录");
        }
        String token = header.substring(7);
        try {
            Claims claims = tokenUtil.parse(token);
            if (!"MEMBER".equals(claims.get("type", String.class))) {
                throw new BizException(ErrorCode.SESSION_EXPIRED, "登录态无效");
            }
            Long memberId = claims.get("memberId", Long.class);
            if (memberId == null) {
                throw new BizException(ErrorCode.SESSION_EXPIRED, "登录态无效");
            }
            Long tenantId = claims.get("tenantId", Long.class);
            if (tenantId == null && tenantFallback != null) {
                tenantId = tenantFallback.apply(memberId);
            }
            return new long[]{memberId, tenantId == null ? DEFAULT_TENANT_ID : tenantId};
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SESSION_EXPIRED, "登录已过期");
        }
    }

    /**
     * 尽力解析 tenantId（用于公开接口）：失败回退默认租户。
     */
    public static Long tryTenantId(HttpServletRequest req, MemberTokenUtil tokenUtil) {
        try {
            return require(req, tokenUtil)[1];
        } catch (Exception e) {
            return DEFAULT_TENANT_ID;
        }
    }
}
