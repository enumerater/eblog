package com.enumerate.eblog.result;

/**
 * 统一响应状态码
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    // ─── 认证 ───
    UNAUTHORIZED(401, "未登录或Token已过期"),
    TOKEN_EXPIRED(4011, "Access Token 已过期"),
    TOKEN_INVALID(4012, "Token 无效"),
    TOKEN_BLACKLISTED(4013, "Token 已被吊销"),
    REFRESH_TOKEN_EXPIRED(4014, "Refresh Token 已过期，请重新登录"),
    FORBIDDEN(403, "无权限访问"),

    // ─── 参数 ───
    BAD_REQUEST(400, "请求参数错误"),
    VALIDATION_FAILED(4001, "参数校验失败"),

    // ─── 业务 ───
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    RATE_LIMITED(429, "请求过于频繁，请稍后再试"),

    // ─── 服务 ───
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),
    ;

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}