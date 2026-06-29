package com.enumerate.auth.controller;

import com.enumerate.auth.service.OAuthService;
import com.enumerate.common.core.result.Result;
import com.enumerate.common.dto.LoginResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * OAuth 认证接口 — GitHub 登录
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oauthService;

    /**
     * GitHub OAuth 登录
     * POST /api/auth/oauth/github
     * Body: { "code": "xxx" }
     */
    @PostMapping("/github")
    public Result<LoginResponseDTO> githubLogin(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return Result.fail(400, "缺少授权码");
        }
        LoginResponseDTO response = oauthService.loginWithGithub(code);
        return Result.success(response);
    }
}