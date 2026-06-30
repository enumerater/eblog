package com.enumerate.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文章事件消息体 — 通过 RocketMQ 从 article-service 推送至下游服务
 *
 * 触发场景:
 *  文章创建/更新/删除 → article-service publish → MQ → intelligence-service (摘要/关键词分析)
 *                                                    → query-service (缓存失效)
 *                                                    → search-service (索引更新)
 *
 * 采用同一个 Topic + eventType 区分事件类型，下游服务按需过滤
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleEventDTO implements Serializable {

    /** 事件类型: ARTICLE_CREATED / ARTICLE_UPDATED / ARTICLE_DELETED */
    private String eventType;

    /** 文章 ID */
    private Long articleId;

    /** 文章标题 */
    private String title;

    /** 文章内容（HTML） */
    private String content;

    /** 文章标签 JSON 数组 */
    private String tagsJson;

    /** 事件发生时间戳 */
    private long timestamp;
}