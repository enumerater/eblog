package com.enumerate.eblog.util;

import lombok.experimental.UtilityClass;

import java.util.UUID;

/**
 * TraceId 生成工具
 * 用于全链路追踪 ID 生成
 */
@UtilityClass
public class TraceIdUtils {

    private static final String TRACE_ID_PREFIX = "eblog-";

    /**
     * 生成全局唯一的 TraceId
     */
    public static String generate() {
        return TRACE_ID_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }
}