package com.enumerate.auth.util;

import com.enumerate.auth.config.JwtProperties;
import com.enumerate.auth.config.KeyPairManager;
import com.enumerate.common.core.constant.CommonConstants;
import com.enumerate.common.dto.JwtPayloadDTO;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT Token 提供者
 *
 * 双 Token 策略:
 *   Access Token  (Access):  短时效, 携带用户身份, 每次请求携带
 *   Refresh Token (Refresh): 长时效, 仅用于刷新 Access Token, 通过 Redis 存储状态
 *
 * 签名算法: RS256 (RSA-SHA256)
 *   - Auth Service: 持有私钥, 负责签发
 *   - Gateway: 持有公钥, 负责验签 (不接触私钥)
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final JwtProperties jwtProperties;

    public JwtTokenProvider(KeyPairManager keyPairManager, JwtProperties jwtProperties) {
        this.privateKey = keyPairManager.getPrivateKey();
        this.publicKey = keyPairManager.getPublicKey();
        this.jwtProperties = jwtProperties;
    }

    /**
     * 生成 Access Token
     */
    public String generateAccessToken(String userId, String username, String role) {
        long now = System.currentTimeMillis();
        long ttl = jwtProperties.getAccessTokenTtlSeconds() * 1000L;

        return Jwts.builder()
                .id(UUID.randomUUID().toString())              // jti: 唯一标识
                .issuer(jwtProperties.getIssuer())             // iss: 签发者
                .subject(userId)                                // sub: 用户ID
                .issuedAt(new Date(now))                       // iat: 签发时间
                .expiration(new Date(now + ttl))               // exp: 过期时间
                .claim("username", username)                    // 用户名
                .claim("role", role)                            // 角色
                .claim("type", CommonConstants.TOKEN_TYPE_ACCESS) // Token 类型
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * 生成 Refresh Token
     * Refresh Token 不包含敏感信息, 仅作为一个 opaque 凭证
     * 真正的状态存储在 Redis 中
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
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * 解析并验证 Token (本服务使用私钥验证)
     * 返回 JwtPayloadDTO (无论 access/refresh)
     */
    public JwtPayloadDTO parseToken(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(publicKey)
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
     * 验证 Token 是否有效 (公开方法, 供 Controller 调用)
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

    /**
     * 获取公钥 (用于注册到 Gateway)
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }
}
