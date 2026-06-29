package com.enumerate.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体 — 支持管理员 + GitHub OAuth 用户
 */
@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 管理员密码 (GitHub 用户为空) */
    private String password;

    /** 角色: admin / editor / user */
    private String role;

    /** 账号状态: 0=禁用 1=启用 */
    @TableField("is_enabled")
    private Boolean enabled;

    // ── GitHub OAuth 字段 ──
    /** GitHub 用户 ID */
    private Long githubId;

    /** GitHub 登录名 */
    private String githubLogin;

    /** GitHub 头像 URL */
    private String avatarUrl;

    /** 显示昵称 (GitHub 取 display name 或 login) */
    private String nickname;

    /** 最后登录 IP */
    private String lastLoginIp;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}