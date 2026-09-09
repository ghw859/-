package com.icbc.lingmou.utils;

import com.icbc.lingmou.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 生成Token、解析Token、校验Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtConfig jwtConfig;

    /**
     * 生成JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     用户角色
     * @return Token字符串
     */
    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getExpiration());

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析Token
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Token解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        return claims.get("username", String.class);
    }

    /**
     * 从Token中获取用户角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        return claims.get("role", String.class);
    }

    /**
     * 校验Token是否有效
     */
    public boolean isTokenValid(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return false;
        return claims.getExpiration().after(new Date());
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
