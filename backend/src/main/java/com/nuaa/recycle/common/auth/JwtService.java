package com.nuaa.recycle.common.auth;

import com.nuaa.recycle.common.error.BizException;
import com.nuaa.recycle.common.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expireMinutes;

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.expire-minutes:120}") long expireMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET 必须至少 32 字节，请在环境变量中设置");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMinutes = expireMinutes;
    }

    public String generate(Long accountId, Role role, Integer tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(accountId))
                .claim("role", role.name())
                .claim("ver", tokenVersion == null ? 0 : tokenVersion)
                .claim("type", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireMinutes * 60)))
                .signWith(key)
                .compact();
    }

    public String generateQrToken(Long userId, Integer tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", Role.USER.name())
                .claim("ver", tokenVersion == null ? 0 : tokenVersion)
                .claim("type", "QR")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(600)))
                .signWith(key)
                .compact();
    }

    public JwtPayload parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            return new JwtPayload(
                    Long.parseLong(claims.getSubject()),
                    Role.valueOf(claims.get("role", String.class)),
                    claims.get("ver", Integer.class),
                    claims.get("type", String.class)
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
    }

    public long expiresInSeconds() {
        return expireMinutes * 60;
    }

    public record JwtPayload(Long id, Role role, Integer version, String type) {
    }
}
