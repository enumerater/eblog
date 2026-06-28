package com.enumerate.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * eblog Auth Service — 认证授权中心
 *
 * 核心功能:
 * 1. 双 Token 认证 (Access Token + Refresh Token)
 * 2. RSA 非对称密钥签发 (私钥签名, Gateway 公钥验签)
 * 3. Token 刷新 (滑动窗口策略: 7天无操作才过期)
 * 4. Token 吊销 (Redis 黑名单 + 布隆过滤器)
 * 5. 操作审计日志
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
