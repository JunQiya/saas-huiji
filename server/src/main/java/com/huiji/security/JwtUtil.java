package com.huiji.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 生成与解析工具(后台用户 token)。
 * token 内含 userId / tenantId / role / storeId / username。
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${huiji-jwt.secret}")
    private String secret;

    @Value("${huiji-jwt.expire-minutes}")
    private long expireMinutes;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成后台用户 token */
    public String generate(Long userId, Long tenantId, String username, String role, Long storeId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMinutes * 60_000L);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("tenantId", tenantId)
                .claim("username", username)
                .claim("role", role)
                .claim("storeId", storeId)
                .claim("type", "USER")
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public long getExpireSeconds() {
        return expireMinutes * 60L;
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /** 校验并返回是否过期/非法 */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
