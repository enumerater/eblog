package com.enumerate.auth.service;

import com.enumerate.auth.entity.User;
import com.enumerate.auth.mapper.UserMapper;
import com.enumerate.auth.util.JwtTokenProvider;
import com.enumerate.common.core.constant.CommonConstants;
import com.enumerate.common.core.exception.BizException;
import com.enumerate.common.core.result.ResultCode;
import com.enumerate.common.dto.LoginResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * GitHub OAuth 登录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${oauth.github.client-id:}")
    private String githubClientId;

    @Value("${oauth.github.client-secret:}")
    private String githubClientSecret;

    private final WebClient webClient = WebClient.create();

    /**
     * GitHub OAuth 登录
     * 前端传回 code, 后端换 access_token, 取用户信息, 创建/查找用户, 签发 JWT
     */
    @Transactional
    public LoginResponseDTO loginWithGithub(String code) {
        if (githubClientId.isBlank() || githubClientSecret.isBlank()) {
            throw new BizException(500, "GitHub OAuth 未配置");
        }

        // 1. 交换 access_token
        Map tokenResponse = webClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .header("Accept", "application/json")
                .bodyValue(Map.of(
                        "client_id", githubClientId,
                        "client_secret", githubClientSecret,
                        "code", code
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (tokenResponse == null || tokenResponse.get("access_token") == null) {
            log.error("GitHub OAuth token 交换失败: {}", tokenResponse);
            throw new BizException(ResultCode.UNAUTHORIZED, "GitHub 登录失败");
        }

        String accessToken = (String) tokenResponse.get("access_token");

        // 2. 获取用户信息
        Map userInfo = webClient.get()
                .uri("https://api.github.com/user")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (userInfo == null || userInfo.get("id") == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "获取 GitHub 用户信息失败");
        }

        Integer githubId = (Integer) userInfo.get("id");
        String githubLogin = (String) userInfo.get("login");
        String avatarUrl = (String) userInfo.get("avatar_url");
        String nickname = (String) userInfo.get("name");
        if (nickname == null || nickname.isBlank()) {
            nickname = githubLogin;
        }

        // 3. 查找或创建用户
        User user = userMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<User>lambdaQuery()
                        .eq(User::getGithubId, githubId.longValue())
                        .last("LIMIT 1"));

        if (user == null) {
            user = new User();
            user.setGithubId(githubId.longValue());
            user.setGithubLogin(githubLogin);
            user.setAvatarUrl(avatarUrl);
            user.setNickname(nickname);
            user.setUsername("github_" + githubLogin);
            user.setRole("user");
            user.setEnabled(true);
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.insert(user);
            log.info("GitHub 新用户注册: githubId={}, login={}", githubId, githubLogin);
        } else {
            // 更新用户信息
            user.setGithubLogin(githubLogin);
            user.setAvatarUrl(avatarUrl);
            user.setNickname(nickname);
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.updateById(user);
            log.info("GitHub 用户登录: userId={}, login={}", user.getId(), githubLogin);
        }

        // 4. 签发 JWT
        String userId = String.valueOf(user.getId());
        String jwtAccessToken = jwtTokenProvider.generateAccessToken(
                userId, user.getUsername(), user.getRole(),
                nickname, avatarUrl);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // 5. 存储 Refresh Token
        String redisKey = CommonConstants.REDIS_KEY_REFRESH_TOKEN + userId;
        redisTemplate.opsForValue().set(
                redisKey, refreshToken,
                Duration.ofSeconds(604800) // 7 天
        );

        return LoginResponseDTO.builder()
                .accessToken(jwtAccessToken)
                .refreshToken(refreshToken)
                .expiresIn(900) // 15 min
                .tokenType("Bearer")
                .build();
    }
}