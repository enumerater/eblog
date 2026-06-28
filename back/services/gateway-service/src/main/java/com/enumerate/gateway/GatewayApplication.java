package com.enumerate.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * eblog Gateway — 统一入口
 *
 * 核心功能:
 * 1. 路由转发: 根据请求路径分发到对应微服务
 * 2. 统一认证: 全局 JWT 验签 (RSA 公钥)
 * 3. 限流熔断: Sentinel 网关流控 + 热点限流
 * 4. 灰度发布: 根据请求头路由到不同版本的服务
 * 5. 跨域配置: CORS 全局统一
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.enumerate.common.feign")
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}