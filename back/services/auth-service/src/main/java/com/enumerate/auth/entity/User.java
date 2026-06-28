package com.enumerate.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员用户实体
 */
@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    /** 角色: admin / editor */
    private String role;

    /** 账号状态: 0=禁用 1=启用 */
    @TableField("is_enabled")
    private Boolean enabled;

    /** 最后登录 IP */
    private String lastLoginIp;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}