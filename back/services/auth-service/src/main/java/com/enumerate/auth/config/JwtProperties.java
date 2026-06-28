package com.enumerate.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** RSA 私钥 (Base64) */
    private String privateKey;

    /** RSA 公钥 (Base64) */
    private String publicKey;

    /** Access Token 有效期 (秒), 默认 15 分钟 */
    private long accessTokenTtlSeconds = 900;

    /** Refresh Token 有效期 (秒), 默认 7 天 */
    private long refreshTokenTtlSeconds = 604800;

    /** 签发者 */
    private String issuer = "eblog-auth";
}