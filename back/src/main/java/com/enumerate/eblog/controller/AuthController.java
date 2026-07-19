package com.enumerate.eblog.controller;

import com.enumerate.eblog.service.AuthService;
import com.enumerate.eblog.result.Result;
import com.enumerate.eblog.dto.LoginRequestDTO;
import com.enumerate.eblog.dto.LoginResponseDTO;
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
     */
    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        return Result.success(response);
    }

    /**
     * 刷新 Access Token
     */
    @PostMapping("/refresh")
    public Result<LoginResponseDTO> refresh(@RequestBody RefreshTokenRequest request) {
        LoginResponseDTO response = authService.refreshAccessToken(request.getRefreshToken());
        return Result.success(response);
    }

    /**
     * 验证 Access Token
     */
    @GetMapping("/verify")
    public Result<?> verify(@RequestParam("token") String token) {
        var payload = authService.verifyAccessToken(token);
        return Result.success(payload);
    }

    /**
     * 登出
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