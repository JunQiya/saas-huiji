package com.huiji.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * H5 会员端 token 工具(独立 memberToken)。
 */
@Component
public class MemberTokenUtil {

    @Value("${huiji-jwt.secret}")
    private String secret;

    @Value("${huiji-jwt.member-expire-minutes}")
    private long expireMinutes;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long memberId, Long tenantId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMinutes * 60_000L);
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("memberId", memberId)
                .claim("tenantId", tenantId)
                .claim("type", "MEMBER")
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
