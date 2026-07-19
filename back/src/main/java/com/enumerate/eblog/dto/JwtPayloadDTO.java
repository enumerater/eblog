package com.enumerate.eblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * JWT 载荷 DTO
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
}