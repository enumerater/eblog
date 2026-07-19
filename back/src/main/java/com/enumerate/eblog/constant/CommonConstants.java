package com.enumerate.eblog.constant;

/**
 * 全局常量定义
 */
public interface CommonConstants {

    // ─── Header 名称 ───
    String HEADER_AUTH       = "Authorization";
    String HEADER_TOKEN_TYPE = "token-type";
    String HEADER_USER_ID    = "X-User-Id";
    String HEADER_USER_ROLE  = "X-User-Role";
    String HEADER_TRACE_ID   = "X-Trace-Id";
    String HEADER_REQUEST_ID = "X-Request-Id";

    // ─── Token 类型 ───
    String TOKEN_TYPE_ACCESS  = "access";
    String TOKEN_TYPE_REFRESH = "refresh";

    // ─── Redis Key 前缀 ───
    String REDIS_KEY_REFRESH_TOKEN = "auth:refresh:";
    String REDIS_KEY_TOKEN_BLACKLIST = "auth:blacklist:";

    // ─── 限流 ───
    String KEY_PREFIX_RATE_LIMIT = "rate_limit:";
}