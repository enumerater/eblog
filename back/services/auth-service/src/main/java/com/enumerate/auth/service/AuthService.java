package com.enumerate.auth.service;

import com.enumerate.auth.config.JwtProperties;
import com.enumerate.auth.entity.User;
import com.enumerate.auth.mapper.UserMapper;
import com.enumerate.auth.util.JwtTokenProvider;
import com.enumerate.common.core.constant.CommonConstants;
import com.enumerate.common.core.exception.BizException;
import com.enumerate.common.core.result.ResultCode;
import com.enumerate.common.dto.JwtPayloadDTO;
import com.enumerate.common.dto.LoginRequestDTO;
import com.enumerate.common.dto.LoginResponseDTO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 认证服务 — 核心业务逻辑
 *
 * 双 Token 流程:
 *
 *   ┌─────────┐                    ┌──────────┐                  ┌──────────┐
 *   │  Client  │  1. POST /login   │  Auth    │  2. Verify      │  MySQL   │
 *   │          │ ─────────────────→│  Service │ ──────────────→ │          │
 *   │          │                   │          │  3. 生成双Token  │          │
 *   │          │ ←──────────────── │          │                  │          │
 *   │          │  {access,refresh} └──────────┘                  └──────────┘
 *   │          │  4. 调用 API 时携带 Access Token (15min)
 *   │          │  5. Access 过期 → POST /refresh → 新 Access
 *   │          │  6. Refresh 过期 → 重新登录
 *   └──────────┘
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 管理员登录
     */
    public LoginResponseDTO login(LoginRequestDTO request) {
        // 1. 查询用户
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, request.getUsername())
                        .last("LIMIT 1"));

        if (user == null) {
            log.warn("登录失败: 用户不存在 username={}", request.getUsername());
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        if (Boolean.FALSE.equals(user.getEnabled())) {
            log.warn("登录失败: 账号已禁用 username={}", request.getUsername());
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        // 2. 验证密码
        if (!user.getPassword().equals(request.getPassword())) {
            log.warn("登录失败: 密码错误 username={}", request.getUsername());
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 3. 生成双 Token
        String userId = String.valueOf(user.getId());
        String accessToken = jwtTokenProvider.generateAccessToken(userId, user.getUsername(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // 4. 存储 Refresh Token 到 Redis (用于吊销和续期)
        String redisKey = CommonConstants.REDIS_KEY_REFRESH_TOKEN + userId;
        redisTemplate.opsForValue().set(
                redisKey,
                refreshToken,
                Duration.ofSeconds(jwtProperties.getRefreshTokenTtlSeconds())
        );

        // 5. 更新登录信息
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户登录成功: userId={}, username={}", userId, user.getUsername());

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getAccessTokenTtlSeconds())
                .tokenType("Bearer")
                .build();
    }

    /**
     * 刷新 Access Token
     *
     * 滑动窗口策略:
     *  - 如果 Refresh Token 还有效 (Redis 中存在), 签发新的 Access Token
     *  - Refresh Token 的 TTL 会重置 (7 天无操作才过期)
     *  - 如果 Refresh Token 已过期 (Redis 中不存在), 要求重新登录
     */
    public LoginResponseDTO refreshAccessToken(String refreshTokenValue) {
        // 1. 验证 Refresh Token 格式
        JwtPayloadDTO payload;
        try {
            payload = jwtTokenProvider.parseToken(refreshTokenValue);
        } catch (Exception e) {
            throw new BizException(ResultCode.TOKEN_INVALID, "Refresh Token 无效");
        }

        // 2. 检查 Redis 中是否存在 (是否已被吊销/过期)
        String redisKey = CommonConstants.REDIS_KEY_REFRESH_TOKEN + payload.getUserId();
        String storedToken = (String) redisTemplate.opsForValue().get(redisKey);

        if (storedToken == null) {
            log.warn("Refresh Token 已过期或不存在: userId={}", payload.getUserId());
            throw new BizException(ResultCode.REFRESH_TOKEN_EXPIRED);
        }

        if (!storedToken.equals(refreshTokenValue)) {
            log.warn("Refresh Token 已被替换, 可能存在 Token 泄露: userId={}", payload.getUserId());
            // 安全策略: 如果发现旧的 Refresh Token 被使用 (说明 Token 可能泄露)
            // 删除所有 Refresh Token, 强制重新登录
            redisTemplate.delete(redisKey);
            throw new BizException(ResultCode.UNAUTHORIZED, "Token 已被使用, 请重新登录");
        }

        // 3. 生成新的 Access Token
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                payload.getUserId(), payload.getUsername(), payload.getRole());

        // 4. 滑动窗口: 重置 Refresh Token 的 TTL
        redisTemplate.expire(redisKey, Duration.ofSeconds(jwtProperties.getRefreshTokenTtlSeconds()));

        log.debug("Token 刷新成功: userId={}", payload.getUserId());

        return LoginResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenValue)  // Refresh Token 不变
                .expiresIn(jwtProperties.getAccessTokenTtlSeconds())
                .tokenType("Bearer")
                .build();
    }

    /**
     * 验证 Access Token
     * 供 Gateway 通过 Feign 调用的兜底方案
     */
    public JwtPayloadDTO verifyAccessToken(String token) {
        JwtPayloadDTO payload = jwtTokenProvider.parseToken(token);

        if (payload.getExpireAt() < System.currentTimeMillis()) {
            throw new BizException(ResultCode.TOKEN_EXPIRED);
        }

        return payload;
    }

    /**
     * 登出: 吊销 Refresh Token
     */
    public void logout(String userId) {
        String redisKey = CommonConstants.REDIS_KEY_REFRESH_TOKEN + userId;
        redisTemplate.delete(redisKey);
        log.info("用户登出: userId={}", userId);
    }

    /**
     * 吊销指定用户的所有 Token
     */
    public void revokeAllTokens(String userId) {
        String redisKey = CommonConstants.REDIS_KEY_REFRESH_TOKEN + userId;
        redisTemplate.delete(redisKey);
        log.info("已吊销用户所有 Token: userId={}", userId);
    }
}
