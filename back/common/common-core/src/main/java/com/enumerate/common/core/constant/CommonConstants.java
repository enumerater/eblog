package com.enumerate.common.core.constant;

/**
 * 全局常量定义
 */
public interface CommonConstants {

    // ─── Header 名称 ───
    String HEADER_AUTH       = "Authorization";
    String HEADER_TOKEN_TYPE = "token-type";       // access / refresh
    String HEADER_USER_ID    = "X-User-Id";
    String HEADER_USER_ROLE  = "X-User-Role";
    String HEADER_NICKNAME   = "X-User-Nickname";
    String HEADER_AVATAR_URL = "X-User-Avatar";
    String HEADER_TRACE_ID   = "X-Trace-Id";
    String HEADER_REQUEST_ID = "X-Request-Id";

    // ─── Token 类型 ───
    String TOKEN_TYPE_ACCESS  = "access";
    String TOKEN_TYPE_REFRESH = "refresh";

    // ─── Redis Key 前缀 ───
    String REDIS_KEY_REFRESH_TOKEN = "auth:refresh:";
    String REDIS_KEY_TOKEN_BLACKLIST = "auth:blacklist:";

    // ─── 服务名称 ───
    String SERVICE_AUTH       = "auth-service";
    String SERVICE_ARTICLE    = "article-service";
    String SERVICE_ARTICLE_QUERY = "article-query-service";
    String SERVICE_COMMENT    = "comment-service";
    String SERVICE_SEARCH     = "search-service";
    String SERVICE_INTELLIGENCE = "intelligence-service";
    String SERVICE_NOTIFICATION = "notification-service";
    String SERVICE_FILE       = "file-service";

    // ─── 限流 ───
    String KEY_PREFIX_RATE_LIMIT = "rate_limit:";

    // ─── 布隆过滤器 ───
    String BLOOM_FILTER_ARTICLE = "bloom:article";
    String BLOOM_FILTER_TOKEN   = "bloom:token";
}
