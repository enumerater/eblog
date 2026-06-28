package com.enumerate.common.feign;

import com.enumerate.common.core.result.Result;
import com.enumerate.common.dto.JwtPayloadDTO;
import com.enumerate.common.feign.fallback.AuthClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Auth Service Feign 客户端
 * Gateway 调用 Auth Service 验证 Token（兜底方案）
 *
 * 正常情况下 Gateway 本地使用 RSA 公钥直接验签
 * 只有本地验签失败且怀疑 Token 时效性问题时才回源查询
 */
@FeignClient(
        name = "auth-service",
        path = "/api/auth",
        fallbackFactory = AuthClientFallbackFactory.class
)
public interface AuthClient {

    /**
     * 验证 Access Token
     */
    @GetMapping("/verify")
    Result<JwtPayloadDTO> verifyToken(@RequestParam("token") String token);

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    Result<JwtPayloadDTO> refreshAccessToken(@RequestParam("refreshToken") String refreshToken);
}