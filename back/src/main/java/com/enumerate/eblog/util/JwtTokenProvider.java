package com.enumerate.eblog.util;

import com.enumerate.eblog.config.JwtProperties;
import com.enumerate.eblog.constant.CommonConstants;
import com.enumerate.eblog.dto.JwtPayloadDTO;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT Token 提供者
 *
 * 使用 HMAC-SHA256 对称签名
 * 只需在 application.properties 中配置 jwt.secret 即可
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token
     */
    public String generateAccessToken(String userId, String username, String role) {
        long now = System.currentTimeMillis();
        long ttl = jwtProperties.getAccessTokenTtlSeconds() * 1000L;

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(jwtProperties.getIssuer())
                .subject(userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttl))
                .claim("username", username)
                .claim("role", role)
                .claim("type", CommonConstants.TOKEN_TYPE_ACCESS)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成 Refresh Token
     */
    public String generateRefreshToken(String userId) {
        long now = System.currentTimeMillis();
        long ttl = jwtProperties.getRefreshTokenTtlSeconds() * 1000L;

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(jwtProperties.getIssuer())
                .subject(userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttl))
                .claim("type", CommonConstants.TOKEN_TYPE_REFRESH)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析并验证 Token
     */
    public JwtPayloadDTO parseToken(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);

        Claims claims = jws.getPayload();

        return JwtPayloadDTO.builder()
                .userId(claims.getSubject())
                .username(claims.get("username", String.class))
                .role(claims.get("role", String.class))
                .jti(claims.getId())
                .expireAt(claims.getExpiration().getTime())
                .build();
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token 无效: {}", e.getMessage());
            return false;
        }
    }
}