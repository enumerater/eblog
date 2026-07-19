package com.enumerate.eblog.config;

import com.enumerate.eblog.constant.CommonConstants;
import com.enumerate.eblog.dto.JwtPayloadDTO;
import com.enumerate.eblog.result.Result;
import com.enumerate.eblog.result.ResultCode;
import com.enumerate.eblog.util.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 *
 * 统一验证 JWT Access Token
 * 公开路径无需认证，其他路径需要 Bearer Token
 */
@Slf4j
@Order(1)
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper;

    /** 公开路径列表 */
    private static final List<String> PUBLIC_PATHS = List.of(
            "POST:/api/auth/login",
            "POST:/api/auth/refresh",
            "GET:/api/articles",
            "GET:/api/articles/**",
            "GET:/api/articles/*/comments",
            "POST:/api/articles/*/comments"
    );

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider,
                         RedisTemplate<String, Object> redisTemplate,
                         ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getRequestURI();

        // 1. 公开路径直接放行
        if (isPublicPath(method, path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. OPTIONS 预检请求放行
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 提取 Token
        String token = extractToken(request);
        if (token == null) {
            writeUnauthorized(response, ResultCode.UNAUTHORIZED);
            return;
        }

        // 4. 验证 Token
        try {
            JwtPayloadDTO payload = jwtTokenProvider.parseToken(token);

            if (payload.getExpireAt() < System.currentTimeMillis()) {
                log.warn("Token 已过期: userId={}", payload.getUserId());
                writeUnauthorized(response, ResultCode.TOKEN_EXPIRED);
                return;
            }

            // 5. 检查 Token 黑名单 (Redis)
            String jti = payload.getJti();
            if (jti != null) {
                Boolean isBlacklisted = redisTemplate.hasKey(
                        CommonConstants.REDIS_KEY_TOKEN_BLACKLIST + jti);
                if (Boolean.TRUE.equals(isBlacklisted)) {
                    log.warn("Token 已被吊销: jti={}", jti);
                    writeUnauthorized(response, ResultCode.TOKEN_BLACKLISTED);
                    return;
                }
            }

            // 6. 通过 → 设置用户信息到请求属性
            request.setAttribute(CommonConstants.HEADER_USER_ID, payload.getUserId());
            request.setAttribute(CommonConstants.HEADER_USER_ROLE, payload.getRole());
            request.setAttribute(CommonConstants.HEADER_TRACE_ID, jti);

            log.debug("Token 验证通过: userId={}, path={}", payload.getUserId(), path);

            filterChain.doFilter(request, response);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Token 已过期: {}", e.getMessage());
            writeUnauthorized(response, ResultCode.TOKEN_EXPIRED);
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("Token 签名无效: {}", e.getMessage());
            writeUnauthorized(response, ResultCode.TOKEN_INVALID);
        } catch (Exception e) {
            log.error("Token 验证异常: ", e);
            writeUnauthorized(response, ResultCode.TOKEN_INVALID);
        }
    }

    private boolean isPublicPath(String method, String path) {
        for (String pattern : PUBLIC_PATHS) {
            String[] parts = pattern.split(":", 2);
            if (parts.length == 2) {
                String patternMethod = parts[0].trim().toUpperCase();
                String patternPath = parts[1].trim();
                if (patternMethod.equals(method) && pathMatcher.match(patternPath, path)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(CommonConstants.HEADER_AUTH);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.fail(resultCode.getCode(), resultCode.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}