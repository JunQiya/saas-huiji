package com.huiji.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 鉴权过滤器: 解析 Authorization 头中的 token, 区分 USER / MEMBER 两类。
 * - USER: 注入 SecurityContext 与 LoginUserHolder
 * - MEMBER: 注入 MemberHolder(供 H5 业务接口使用)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MemberTokenUtil memberTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LoginUserHolder.clear();
        MemberHolder.clear();
        SecurityContextHolder.clearContext();

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                // 优先按 USER token 解析(后端管理端)
                Claims claims;
                try {
                    claims = jwtUtil.parse(token);
                } catch (Exception ignore) {
                    // USER token 解析失败, 再尝试 MEMBER token(H5 会员端)
                    claims = memberTokenUtil.parse(token);
                }
                String type = claims.get("type", String.class);
                if ("USER".equals(type)) {
                    Long userId = claims.get("userId", Long.class);
                    Long tenantId = claims.get("tenantId", Long.class);
                    String username = claims.get("username", String.class);
                    String role = claims.get("role", String.class);
                    Long storeId = claims.get("storeId", Long.class);
                    if (userId != null) {
                        LoginUser user = LoginUser.builder()
                                .userId(userId)
                                .tenantId(tenantId)
                                .username(username)
                                .role(role)
                                .storeId(storeId)
                                .build();
                        LoginUserHolder.set(user);
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } else if ("MEMBER".equals(type)) {
                    Long memberId = claims.get("memberId", Long.class);
                    if (memberId != null) {
                        MemberHolder.set(memberId);
                    }
                }
            } catch (Exception e) {
                // token 非法或过期, 不设置上下文, 由后续安全链处理
                log.debug("token 解析失败: {}", e.getMessage());
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            LoginUserHolder.clear();
            MemberHolder.clear();
        }
    }
}
