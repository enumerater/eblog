package com.enumerate.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * JWT 载荷 DTO
 * Gateway 解析 Token 后，将用户信息放入请求头转发给下游
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtPayloadDTO implements Serializable {
    private String userId;
    private String username;
    private String role;
    private String jti;          // JWT ID (用于黑名单)
    private long expireAt;       // 过期时间戳
    private String nickname;     // 显示昵称 (GitHub OAuth)
    private String avatarUrl;    // 头像 URL (GitHub OAuth)
}