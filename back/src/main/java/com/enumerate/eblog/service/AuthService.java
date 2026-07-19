package com.enumerate.eblog.service;

import com.enumerate.eblog.config.JwtProperties;
import com.enumerate.eblog.constant.CommonConstants;
import com.enumerate.eblog.dto.JwtPayloadDTO;
import com.enumerate.eblog.dto.LoginRequestDTO;
import com.enumerate.eblog.dto.LoginResponseDTO;
import com.enumerate.eblog.entity.User;
import com.enumerate.eblog.exception.BizException;
import com.enumerate.eblog.mapper.UserMapper;
import com.enumerate.eblog.result.ResultCode;
import com.enumerate.eblog.util.JwtTokenProvider;
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
 *   Client 登录 → 获取 {accessToken, refreshToken}
 *   调用 API 时携带 Access Token (15min)
 *   Access 过期 → POST /api/auth/refresh → 新 Access
 *   Refresh 过期 → 重新登录
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

        if (!user.getPassword().equals(request.getPassword())) {
            log.warn("登录失败: 密码错误 username={}", request.getUsername());
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        String userId = String.valueOf(user.getId());
        String accessToken = jwtTokenProvider.generateAccessToken(userId, user.getUsername(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        String redisKey = CommonConstants.REDIS_KEY_REFRESH_TOKEN + userId;
        redisTemplate.opsForValue().set(
                redisKey,
                refreshToken,
                Duration.ofSeconds(jwtProperties.getRefreshTokenTtlSeconds())
        );

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
     * 刷新 Access Token (滑动窗口策略)
     */
    public LoginResponseDTO refreshAccessToken(String refreshTokenValue) {
        JwtPayloadDTO payload;
        try {
            payload = jwtTokenProvider.parseToken(refreshTokenValue);
        } catch (Exception e) {
            throw new BizException(ResultCode.TOKEN_INVALID, "Refresh Token 无效");
        }

        String redisKey = CommonConstants.REDIS_KEY_REFRESH_TOKEN + payload.getUserId();
        String storedToken = (String) redisTemplate.opsForValue().get(redisKey);

        if (storedToken == null) {
            log.warn("Refresh Token 已过期或不存在: userId={}", payload.getUserId());
            throw new BizException(ResultCode.REFRESH_TOKEN_EXPIRED);
        }

        if (!storedToken.equals(refreshTokenValue)) {
            log.warn("Refresh Token 已被替换, 可能存在 Token 泄露: userId={}", payload.getUserId());
            redisTemplate.delete(redisKey);
            throw new BizException(ResultCode.UNAUTHORIZED, "Token 已被使用, 请重新登录");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                payload.getUserId(), payload.getUsername(), payload.getRole());

        redisTemplate.expire(redisKey, Duration.ofSeconds(jwtProperties.getRefreshTokenTtlSeconds()));

        log.debug("Token 刷新成功: userId={}", payload.getUserId());

        return LoginResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtProperties.getAccessTokenTtlSeconds())
                .tokenType("Bearer")
                .build();
    }

    /**
     * 验证 Access Token
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