package com.nei10u.tip.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * JWT 服务：生成/解析 Token
 *
 * 说明：
 * - 当前版本主要用于“签发 JWT 并回传给客户端”
 * - 未强制接入 Spring Security 过滤器（避免一次性引入过多改动）
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.ttl-seconds:2592000}") long ttlSeconds
    ) {
        // HMAC key 需要足够长度；不足时仍可工作但安全性下降
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
    }

    public String issueToken(long userId, Map<String, Object> extraClaims) {
        final Instant now = Instant.now();
        final Instant exp = now.plusSeconds(ttlSeconds);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(extraClaims == null ? Map.of() : extraClaims)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}

