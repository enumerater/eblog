package com.enumerate.auth.controller;

import com.enumerate.auth.service.AuthService;
import com.enumerate.common.core.result.Result;
import com.enumerate.common.dto.LoginRequestDTO;
import com.enumerate.common.dto.LoginResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 管理员登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        return Result.success(response);
    }

    /**
     * 刷新 Access Token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public Result<LoginResponseDTO> refresh(@RequestBody RefreshTokenRequest request) {
        LoginResponseDTO response = authService.refreshAccessToken(request.getRefreshToken());
        return Result.success(response);
    }

    /**
     * 验证 Access Token (Gateway 兜底调用)
     * GET /api/auth/verify?token=xxx
     */
    @GetMapping("/verify")
    public Result<?> verify(@RequestParam("token") String token) {
        var payload = authService.verifyAccessToken(token);
        return Result.success(payload);
    }

    /**
     * 登出
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-User-Id") String userId) {
        authService.logout(userId);
        return Result.success();
    }

    /**
     * 内部 DTO: 刷新 Token 请求
     */
    public static class RefreshTokenRequest {
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}