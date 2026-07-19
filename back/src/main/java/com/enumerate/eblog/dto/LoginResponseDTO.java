package com.enumerate.eblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录响应 DTO
 * 包含双 Token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO implements Serializable {
    private String accessToken;   // 短时效 Token (15min)
    private String refreshToken;  // 长时效 Token (7d)
    private long expiresIn;       // Access Token 有效期 (秒)
    private String tokenType;     // Bearer
}