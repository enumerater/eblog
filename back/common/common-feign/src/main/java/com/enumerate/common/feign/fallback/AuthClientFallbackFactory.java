package com.enumerate.common.feign.fallback;

import com.enumerate.common.core.result.Result;
import com.enumerate.common.core.result.ResultCode;
import com.enumerate.common.dto.JwtPayloadDTO;
import com.enumerate.common.feign.AuthClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * AuthClient 熔断降级工厂
 * 当 Auth Service 不可用时，返回 Token 验证失败
 * 避免因 Auth 服务故障导致整个链路阻塞
 */
@Slf4j
@Component
public class AuthClientFallbackFactory implements FallbackFactory<AuthClient> {

    @Override
    public AuthClient create(Throwable cause) {
        log.error("AuthClient 调用失败, 触发熔断降级: {}", cause.getMessage());
        return new AuthClient() {
            @Override
            public Result<JwtPayloadDTO> verifyToken(String token) {
                return Result.fail(ResultCode.SERVICE_UNAVAILABLE.getCode(), "认证服务暂不可用, 请稍后再试");
            }

            @Override
            public Result<JwtPayloadDTO> refreshAccessToken(String refreshToken) {
                return Result.fail(ResultCode.SERVICE_UNAVAILABLE.getCode(), "认证服务暂不可用");
            }
        };
    }
}