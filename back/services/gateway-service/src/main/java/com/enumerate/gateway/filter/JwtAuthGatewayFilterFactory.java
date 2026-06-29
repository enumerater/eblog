package com.enumerate.gateway.filter;

import com.enumerate.common.core.constant.CommonConstants;
import com.enumerate.common.core.result.Result;
import com.enumerate.common.core.result.ResultCode;
import com.enumerate.common.core.util.RsaKeyUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.List;

/**
 * JWT 认证过滤器 (GatewayFilter)
 *
 * 作用:
 *  在 Gateway 层统一验证 JWT Access Token
 *  使用 RSA 公钥本地验签 (无需调用 Auth Service)
 *
 * 处理流程:
 *  1. 判断请求路径是否在公开路径列表中 → 直接放行
 *  2. 从 Authorization Header 提取 Token
 *  3. 使用 RSA 公钥验签 + 解析 Payload
 *  4. 检查 Token 是否在黑名单中 (Redis)
 *  5. 通过 → 将用户信息设置到请求头, 转发给下游
 *  6. 失败 → 返回 401 JSON
 *
 * 配置示例 (application.yml):
 *  filters:
 *    - name: JwtAuth
 *      args:
 *        publicPaths: "GET:/api/articles,GET:/api/articles/**"
 */
@Slf4j
@Component
public class JwtAuthGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthGatewayFilterFactory.Config> {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private volatile PublicKey publicKey;
    private final String publicKeyBase64;

    public JwtAuthGatewayFilterFactory(
            ReactiveRedisTemplate<String, Object> redisTemplate,
            @org.springframework.beans.factory.annotation.Value("${jwt.public-key:}") String publicKeyBase64) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
        this.publicKeyBase64 = publicKeyBase64;
        if (StringUtils.hasText(publicKeyBase64)) {
            this.publicKey = RsaKeyUtils.publicKeyFromBase64(publicKeyBase64);
        }
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            String method = request.getMethod().name();
            String path = request.getURI().getPath();


            // 1. 检查是否为公开路径
            if (isPublicPath(method, path, config.getPublicPaths())) {
                return chain.filter(exchange);
            }

            // 2. 处理 OPTIONS 预检请求
            if (HttpMethod.OPTIONS.matches(method)) {
                return chain.filter(exchange);
            }

            // 3. 提取 Token
            String token = extractToken(request);
            if (token == null) {
                log.warn("缺少 Token: method={}, path={}", method, path);
                return unauthorized(response, ResultCode.UNAUTHORIZED);
            }

            // 4. 确保公钥已加载
            if (publicKey == null) {
                log.error("JWT 公钥未配置, 请求被拒绝");
                return unauthorized(response, ResultCode.SERVICE_UNAVAILABLE);
            }

            // 5. 验签 + 解析 Payload
            final Claims claims;
            try {
                Jws<Claims> jws = Jwts.parser()
                        .verifyWith(publicKey)
                        .build()
                        .parseSignedClaims(token);

                claims = jws.getPayload();

                // 5a. 检查过期
                if (claims.getExpiration().getTime() < System.currentTimeMillis()) {
                    log.warn("Token 已过期: sub={}", claims.getSubject());
                    return unauthorized(response, ResultCode.TOKEN_EXPIRED);
                }
            } catch (SignatureException e) {
                log.warn("Token 签名无效: {}", e.getMessage());
                return unauthorized(response, ResultCode.TOKEN_INVALID);
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                log.warn("Token 已过期: {}", e.getMessage());
                return unauthorized(response, ResultCode.TOKEN_EXPIRED);
            } catch (Exception e) {
                log.error("Token 验证异常: ", e);
                return unauthorized(response, ResultCode.TOKEN_INVALID);
            }

            // 6. 检查 Token 黑名单 (Redis) — 纯响应式，不 block
            String jti = claims.getId();
            if (jti == null) {
                // 无 jti，跳过 Redis 检查，直接放行
                return chain.filter(withUserHeaders(exchange, request, claims, null));
            }

            return redisTemplate
                    .opsForValue()
                    .get(CommonConstants.REDIS_KEY_TOKEN_BLACKLIST + jti)
                    .hasElement()
                    .onErrorReturn(false)       // Redis 不可用时默认放行（JWT 验签已通过）
                    .flatMap(isBlacklisted -> {
                        if (Boolean.TRUE.equals(isBlacklisted)) {
                            log.warn("Token 已被吊销: jti={}", jti);
                            return unauthorized(response, ResultCode.TOKEN_BLACKLISTED);
                        }

                        log.debug("Token 验证通过: userId={}, path={}",
                                claims.getSubject(), path);

                        return chain.filter(withUserHeaders(exchange, request, claims, jti));
                    });
        };
    }

    /**
     * 将用户信息设置到请求头，转发给下游
     */
    private ServerWebExchange withUserHeaders(ServerWebExchange exchange,
                                              ServerHttpRequest request,
                                              Claims claims, String jti) {
        ServerHttpRequest mutated = request.mutate()
                .header(CommonConstants.HEADER_USER_ID, claims.getSubject())
                .header(CommonConstants.HEADER_USER_ROLE,
                        claims.get("role", String.class))
                .build();
        if (jti != null) {
            mutated = mutated.mutate()
                    .header(CommonConstants.HEADER_TRACE_ID, jti)
                    .build();
        }
        return exchange.mutate().request(mutated).build();
    }

    /**
     * 检查请求路径是否在公开路径列表中
     */
    private boolean isPublicPath(String method, String path, List<String> publicPaths) {
        if (publicPaths == null || publicPaths.isEmpty()) {
            return false;
        }
        for (String pattern : publicPaths) {
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

    /**
     * 从请求头提取 Bearer Token
     */
    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(CommonConstants.HEADER_AUTH);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 返回 401 JSON 响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, ResultCode resultCode) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<Void> result = Result.fail(resultCode.getCode(), resultCode.getMessage());
        DataBufferFactory factory = response.bufferFactory();
        byte[] bytes = result.toString().getBytes(StandardCharsets.UTF_8);
        // 使用 Jackson 序列化
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            bytes = mapper.writeValueAsBytes(result);
        } catch (Exception e) {
            // fallback
        }
        DataBuffer buffer = factory.wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Data
    public static class Config {
        /**
         * 公开路径列表, 格式: "GET:/api/articles,GET:/api/articles/**"
         */
        private List<String> publicPaths;
    }
}